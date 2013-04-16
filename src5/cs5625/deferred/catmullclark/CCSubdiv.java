package cs5625.deferred.catmullclark;

import java.util.Set;

import cs5625.deferred.datastruct.*;
import cs5625.deferred.scenegraph.Mesh;

public class CCSubdiv {
	
	private Mesh mMesh;
	
	public CCSubdiv(EdgeDS edgeDS)
	{
		//Fill in this function to perform catmull clark subdivision
		
		//Grab the list of all the vert, edge, polygon IDs.
		Set<Integer> polygonIDs = edgeDS.getPolygonIDs();
		Set<Integer> edgeIDs = edgeDS.getEdgeIDs();
		Set<Integer> vertIDs = edgeDS.getVertexIDs();
		
		//Create the new vertices according to the proper generation rules.
		
		//First create odd face verts
		for (Integer i: polygonIDs ) {
			PolygonData pd = edgeDS.getPolygonData(i);
			
			
		}
		
		//Then create odd edge verts
		for (Integer i: edgeIDs) {
			EdgeData ed = edgeDS.getEdgeData(i);
		}
		
		//Then create new even verts (move the even verts).
		for (Integer i: vertIDs) {
			VertexData vd = edgeDS.getVertexData(i);
		}
		
		
		//Finally create a new mesh out of the existing
		
		
		this.mMesh = edgeDS.getMesh();
		
		
		
	}
	
	public Mesh getNewMesh()
	{
		return this.mMesh;
	}
	
}
