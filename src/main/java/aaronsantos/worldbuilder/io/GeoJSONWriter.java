package aaronsantos.worldbuilder.io;

import aaronsantos.worldbuilder.WorldSnapShot;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Writes a WorldSnapShot to GEOJson format
 */
public class GeoJSONWriter
{
    public final void write(final WorldSnapShot snapShot) throws IOException
    {
        final Date now = new Date();
        final String path = String.format("SnapShot-%s.json", now.toString()).replace(" ", "-");
        final JsonFactory factory = new JsonFactory();
        final JsonGenerator generator = factory.createJsonGenerator(new File(path), JsonEncoding.UTF8);
        generator.setCodec(new ObjectMapper());

        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

        final ArrayNode features = nodeFactory.arrayNode();
        features.add(makeElevationFeature(snapShot.getWorldData()));

        ObjectNode featureCollection = nodeFactory.objectNode();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        try
        {
            generator.writeTree(featureCollection);
        }
        finally
        {
            generator.flush();
            generator.close();
        }
    }
    
    private ObjectNode makeElevationFeature(final float[][] data)
    {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        
        ObjectNode elevationProperties = nodeFactory.objectNode();
        elevationProperties.put("type", "Elevation");

        ObjectNode elevationFeature = nodeFactory.objectNode();
        elevationFeature.put("type", "Feature");
        elevationFeature.put("geometry", makeGeometry(data));
        
        elevationFeature.put("properties", elevationProperties);
        return elevationFeature;
    }
    
    private ObjectNode makeGeometry(final float[][] data)
    {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

        ObjectNode geometry = nodeFactory.objectNode();
        geometry.put("type", "MultiPolygon");
        ArrayNode coordinates = geometry.putArray("coordinates");
        for (int i = 0; i < data.length-1; i++)
        {
            for (int j = 0; j < data[i].length-1; j++)
            {
                ArrayNode polygon = coordinates.addArray();
                ArrayNode p0 = polygon.addArray();
                ArrayNode p1 = polygon.addArray();
                ArrayNode p2 = polygon.addArray();
                ArrayNode p3 = polygon.addArray();

                p0.add(i);
                p0.add(j);
                p0.add(data[i][j]);

                p1.add(i+1);
                p1.add(j);
                p1.add(data[i+1][j]);

                p2.add(i+1);
                p2.add(j+1);
                p2.add(data[i+1][j+1]);

                p3.add(i);
                p3.add(j+1);
                p3.add(data[i][j+1]);
            }
        }
        return geometry;
    }
}
