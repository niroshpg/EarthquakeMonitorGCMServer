package com.niroshpg.android.gcm;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class SendMessageServletTest {
	
	private SendMessageServletMock sendMessageServletMock;
	
	private class SendMessageServletMock extends SendMessageServlet{

		private static final long serialVersionUID = 1L;

		public boolean invokeLocationWithinPolygonBounds(Polygon polygon, String str)
		{
			return isLocationWithinPolygonBounds(polygon,str);
		}
	}
	
	
	@Before
	public void setup()
	{
		sendMessageServletMock = new SendMessageServletMock();
	}
	
	@After
	public void tearDown()
	{
		
	}

	@Test
	public void isLocationWithinPolygonBoundsTest()
	{
		final GeometryFactory geometryFactory = new GeometryFactory();

	    final ArrayList<Coordinate> points = new ArrayList<Coordinate>();
	    
	    points.add(new Coordinate(1,1));
	    points.add(new Coordinate(1, 10));
	    points.add(new Coordinate(5, 10));
	    points.add(new Coordinate(5, 1));
	    points.add(new Coordinate(1, 1));
	    
		final Polygon polygon = geometryFactory.createPolygon(new LinearRing(new CoordinateArraySequence(points
			        .toArray(new Coordinate[points.size()])), geometryFactory), null);

		assertTrue(sendMessageServletMock.invokeLocationWithinPolygonBounds(polygon, "3N, 8E"));
		
		assertFalse(sendMessageServletMock.invokeLocationWithinPolygonBounds(polygon, "8N, 8E"));
	}
}
