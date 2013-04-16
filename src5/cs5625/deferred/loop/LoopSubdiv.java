package cs5625.deferred.loop;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import javax.vecmath.Point2f;
import javax.vecmath.Point3f;

import cs5625.deferred.datastruct.EdgeDS;
import cs5625.deferred.datastruct.EdgeData;
import cs5625.deferred.datastruct.PolygonData;
import cs5625.deferred.datastruct.VertexAttributeData;
import cs5625.deferred.datastruct.VertexData;
import cs5625.deferred.misc.TrimeshMaker;
import cs5625.deferred.scenegraph.Mesh;

/**
 * LoopSubdiv.java
 * 
 * Perform the subdivision in this class/package 
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Rohit Garg (rg534)
 * @date 2012-03-23
 */

public class LoopSubdiv {
	
	private Mesh mMesh;
	
	public LoopSubdiv(EdgeDS edgeDS)
	{
		TreeMap<Integer, VertexAttributeData> newVerts = new TreeMap<Integer, VertexAttributeData>();
		int newVertID = 0;
		// TODO PA5: Fill in this function to perform loop subdivision
		//this.mMesh = edgeDS.getMesh();
		// Loop through every edge
		for(Integer edgeID : edgeDS.getEdgeIDs()) {
			EdgeData edge = edgeDS.getEdgeData(edgeID);
			ArrayList<Integer> leftEdges = edgeDS.getOtherEdgesOfLeftFace(edgeID);
			ArrayList<Integer> rightEdges = edgeDS.getOtherEdgesOfRightFace(edgeID);
			VertexAttributeData vert0 = edgeDS.getVertexData(edge.getVertex0()).mData;
			VertexAttributeData vert1 = edgeDS.getVertexData(edge.getVertex1()).mData;
			VertexAttributeData leftVert = null;
			VertexAttributeData rightVert = null;
			// If there is a left polygon
			if (leftEdges.size() > 0) {
				EdgeData leftEdge = edgeDS.getEdgeData(leftEdges.get(0));
				// Grab the third vert from the left polygon
				if (leftEdge.getVertex0() != edge.getVertex0())
					leftVert = edgeDS.getVertexData(leftEdge.getVertex0()).mData;
				else
					leftVert = edgeDS.getVertexData(leftEdge.getVertex1()).mData;
			}
			// If there is a right polygon
			if (rightEdges.size() > 0) {
				EdgeData rightEdge = edgeDS.getEdgeData(rightEdges.get(0));
				// Grab the third vert from the right polygon
				if (rightEdge.getVertex0() != edge.getVertex0())
					rightVert = edgeDS.getVertexData(rightEdge.getVertex0()).mData;
				else
					rightVert = edgeDS.getVertexData(rightEdge.getVertex1()).mData;
			}
			if (leftVert != null && rightVert != null) { //normal case
				Point3f pos = new Point3f(vert0.getPosition());
				pos.scale(3f/8f);
				Point3f temp = new Point3f(vert1.getPosition());
				temp.scale(3f/8f);
				pos.add(temp);
				temp = new Point3f(leftVert.getPosition());
				temp.scale(1f/8f);
				pos.add(temp);
				temp = new Point3f(rightVert.getPosition());
				temp.scale(1f/8f);
				pos.add(temp);
				Point2f tex = new Point2f(vert0.getTexCoord());
				tex.scale(3f/8f);
				Point2f temp1 = new Point2f(vert1.getTexCoord());
				temp1.scale(3f/8f);
				tex.add(temp1);
				temp1 = new Point2f(leftVert.getTexCoord());
				temp1.scale(1f/8f);
				tex.add(temp1);
				temp1 = new Point2f(rightVert.getTexCoord());
				temp1.scale(1f/8f);
				tex.add(temp1);
				newVerts.put(newVertID, new VertexAttributeData(pos, tex));
				edge.setVertexIDNew(newVertID++);
			}
			else { //boundary case
				Point3f pos = new Point3f(vert0.getPosition());
				pos.scale(.5f);
				Point3f temp = new Point3f(vert1.getPosition());
				temp.scale(.5f);
				pos.add(temp);
				Point2f tex = new Point2f(vert0.getTexCoord());
				tex.scale(.5f);
				Point2f temp1 = new Point2f(vert1.getTexCoord());
				temp1.scale(.5f);
				tex.add(temp1);
				newVerts.put(newVertID, new VertexAttributeData(pos, tex));
				edge.setVertexIDNew(newVertID++);
			}
		}
		
		// Loop through every (even) vertex
		for (Integer vertID : edgeDS.getVertexIDs()) {
			VertexData vert = edgeDS.getVertexData(vertID);
			// Grab all the connected edges
			ArrayList<Integer> edges = vert.getConnectedEdges();
			Point3f pos = new Point3f(vert.mData.getPosition());
			Point2f tex = new Point2f(vert.mData.getTexCoord());
			float scalar;
			// Set the scalar depending on the number of edges
			if (edges.size() == 2) { // boundary case
				scalar = 1f/8f;
			}
			else if (edges.size() == 3) { // special case
				scalar = 3f/16f;
			}
			else { // normal case
				scalar = 3f / (8f * (float)edges.size());
			}
			pos.scale(1f-(scalar*(float)edges.size()));
			tex.scale(1f-(scalar*(float)edges.size()));
			// for each of the connected edges
			for(Integer edgeID : edges) {
				EdgeData edge = edgeDS.getEdgeData(edgeID);
				// Grab the data for the newly created vertex in the middle of the edge
				VertexAttributeData vData = newVerts.get(edge.getNewVertexID());
				// Scale the new vertex pos/tex
				Point3f temp = new Point3f(vData.getPosition());
				temp.scale(scalar);
				pos.add(temp);
				Point2f temp1 = new Point2f(vData.getTexCoord());
				temp1.scale(scalar);
				tex.add(temp1);
			}
			// Create a new vertex
			newVerts.put(newVertID, new VertexAttributeData(pos, tex));
			vert.setNewVertexID(newVertID++);		
		}
		
		// Create the Trimesh
		Set<Integer> polys = edgeDS.getPolygonIDs();
		TrimeshMaker tri = new TrimeshMaker(newVertID, polys.size() * 4, 0);
		for (Integer polyID : polys) {
			PolygonData poly = edgeDS.getPolygonData(polyID);
			ArrayList<Integer> verts = poly.getAllVertices();
			ArrayList<Integer> edges = poly.getAllEdges();
			for(Integer vertID : verts) {
				VertexData vert = edgeDS.getVertexData(vertID);
				ArrayList<Integer> connectedVerts = new ArrayList<Integer>();
				for(Integer edgeID : vert.getConnectedEdges()) {
					if (edges.contains(edgeID)) {
						connectedVerts.add(edgeDS.getEdgeData(edgeID).getNewVertexID());
					}
				}
				if (connectedVerts.size() != 2) {
					System.out.println("This is not a triangle!!!");
				}
				else {
					tri.addTriangle(newVerts.get(vert.getNewVertexID()).getPosition(), 
						newVerts.get(connectedVerts.get(0)).getPosition(), 
						newVerts.get(connectedVerts.get(1)).getPosition(), 
						newVerts.get(vert.getNewVertexID()).getTexCoord(),
						newVerts.get(connectedVerts.get(0)).getTexCoord(),
						newVerts.get(connectedVerts.get(1)).getTexCoord(),
						vert.getNewVertexID(), 
						connectedVerts.get(0), 
						connectedVerts.get(1));
				}
			}
			ArrayList<Integer> connectedVerts = new ArrayList<Integer>();
			for(Integer edgeID : edges) {
				connectedVerts.add(edgeDS.getEdgeData(edgeID).getNewVertexID());
			}
			if (connectedVerts.size() != 3) {
				System.out.println("This polygon is not a triangle!!!!!");
			}
			else {
				tri.addTriangle(newVerts.get(connectedVerts.get(0)).getPosition(), 
						newVerts.get(connectedVerts.get(1)).getPosition(), 
						newVerts.get(connectedVerts.get(2)).getPosition(), 
						newVerts.get(connectedVerts.get(0)).getTexCoord(),
						newVerts.get(connectedVerts.get(1)).getTexCoord(),
						newVerts.get(connectedVerts.get(2)).getTexCoord(),
						connectedVerts.get(0), 
						connectedVerts.get(1), 
						connectedVerts.get(2));
			}
			
		}
		this.mMesh = tri.getMesh();
		
		
	}
	
	public Mesh getNewMesh()
	{
		return this.mMesh;
	}
	
}
