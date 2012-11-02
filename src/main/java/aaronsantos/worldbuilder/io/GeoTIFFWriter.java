package aaronsantos.worldbuilder.io;

import aaronsantos.worldbuilder.WorldSnapShot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.relaxng.datatype.Datatype;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.geotiff.GeotiffWriter;

/**
 * Writes a WorldSnapShot to GEOJson format
 */
public class GeoTIFFWriter
{
    public final void write(final WorldSnapShot snapShot) throws IOException
    {
        final Date now = new Date();
        final String path = String.format("SnapShot-%s.tiff", now.toString()).replace(" ", "-");
        final GeotiffWriter writer = new GeotiffWriter(path);
        try
        {
            Formatter errlog = new Formatter();
            final String location = "";
            NetcdfDataset ds = new NetcdfDataset();

            float[][] data = snapShot.getWorldData();

            GridDataset dataset = new ucar.nc2.dt.grid.GridDataset(ds);
            Collection<CoordinateAxis> axes = new ArrayList<CoordinateAxis>();
            CoordSysBuilder csb = new CoordSysBuilder();
            csb.buildCoordinateSystems(ds);
            final Group group = new Group(new NetcdfFile(){}, null, "root");
            final Structure seq = new Sequence(ds, group, null, "root");
            VariableDS vds = new VariableDS(ds, group, seq, "elevation",
                DataType.FLOAT, "1", "m", "Elevation variable DS");
            CoordinateAxis1D lat = new CoordinateAxis1D(ds, vds);
            lat.setAxisType(AxisType.Lat);
            lat.setDimensions(Integer.toString(data.length));
            lat.resetShape();
            lat.setValues(data.length, 0, 1);
            lat.setFillValueIsMissing(true);
            CoordinateAxis1D lon = new CoordinateAxis1D(ds, vds);
            lon.setAxisType(AxisType.Lon);
            lon.setDimensions(Integer.toString(data[0].length));
            lon.resetShape();
            lon.setValues(data[0].length, 0, 1);
            lon.setFillValueIsMissing(true);
            axes.add(lat);
            axes.add(lon);
            Collection<CoordinateTransform> coordTrans = new ArrayList<CoordinateTransform>();
            CoordinateSystem cs = new CoordinateSystem(ds, axes, coordTrans);
            GridCoordSys gcs = new GridCoordSys(cs, errlog);
            GridDatatype grid = new GeoGrid(dataset, vds, gcs);

            final ArrayFloat.D2 af = new ArrayFloat.D2(data.length, data[0].length);
            for (int i= 0; i < data.length; i++)
            {
                for (int j = 0; j < data[i].length; j++)
                {
                    af.set(i, j, data[i][j]);
                }
            }
            writer.writeGrid(dataset, grid, af, true);
        }
        finally
        {
            writer.close();
        }
    }
}
