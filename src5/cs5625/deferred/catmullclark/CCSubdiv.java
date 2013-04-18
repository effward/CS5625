package cs5625.deferred.catmullclark;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Set;

import javax.vecmath.Point2f;
import javax.vecmath.Point3f;

import cs5625.deferred.datastruct.*;
import cs5625.deferred.scenegraph.Mesh;
import cs5625.deferred.scenegraph.Quadmesh;

public class CCSubdiv {
	
	private Mesh mMesh;
	
	public CCSubdiv(EdgeDS edgeDS)
	{
		//Fill in this function to perform catmull clark subdivision
		
		//Grab the list of all the vert, edge, polygon IDs.
		Set<Integer> vertIDs = edgeDS.getVertexIDs();
		Set<Integer> edgeIDs = edgeDS.getEdgeIDs();
		Set<Integer> polygonIDs = edgeDS.getPolygonIDs();
		
		//Create new lists for verts, edges, polygons.
		ArrayList<Vert> verts = new ArrayList<Vert>(vertIDs.size());
		ArrayList<Edge> edges = new ArrayList<Edge>(edgeIDs.size());
		ArrayList<Poly> polys = new ArrayList<Poly>(polygonIDs.size());
		
		//Copy over all the old verts into the new list in the same order.
		for (Integer i: vertIDs) {
			VertexData vd = edgeDS.getVertexData(i);
			Point3f pos = vd.mData.getPosition();
			Point2f tex = vd.mData.getTexCoord();
			verts.add(new Vert(pos.x, pos.y, pos.z, tex.x, tex.y));
		}
		
		//Create the new vertices according to the proper generation rules.
		
		//First create odd face verts
		for (Integer i: polygonIDs ) {
			PolygonData pd = edgeDS.getPolygonData(i);
			ArrayList<Integer> pdvd = pd.getAllVertices();
			float over = 1.0f / (float)pdvd.size();
			
			Point3f pos = new Point3f();
			Point2f tex = new Point2f();
			
			//Average the surrounding points...
			for (Integer j: pdvd) {
				pos.add(edgeDS.getVertexData(j).mData.getPosition());
				tex.add(edgeDS.getVertexData(j).mData.getTexCoord());
			}
			
			pos.scale(over);
			tex.scale(over);
			
			//Create new point there.
			Vert point = new Vert(pos.x, pos.y, pos.z, tex.x, tex.y);
			verts.add(point);
			pd.setNewFaceVertexID(verts.size() - 1); //store new vert index in face vert id.
			
			//Keep track of odd face verts for each even vert.
			for (Integer j: pdvd) {
				verts.get(j).oddFaceVerts.add(point);
			}
		}
		
		//Then create odd edge verts
		for (Integer i: edgeIDs) {
			EdgeData ed = edgeDS.getEdgeData(i);
			Point3f v1 = edgeDS.getVertexData(ed.getVertex0()).mData.getPosition();
			Point3f v2 = edgeDS.getVertexData(ed.getVertex1()).mData.getPosition();
			
			Point2f v1t = edgeDS.getVertexData(ed.getVertex0()).mData.getTexCoord();
			Point2f v2t = edgeDS.getVertexData(ed.getVertex1()).mData.getTexCoord();
			
			if (edgeDS.isCreaseEdge(i)) {
				//It's a  crease, so just average the two end verts of the edge
				Vert point = new Vert(
					(v1.x + v2.x) * 0.5f,
					(v1.y + v2.y) * 0.5f,
					(v1.z + v2.z) * 0.5f,
					(v1t.x + v2t.x) * 0.5f,
					(v1t.y + v2t.y) * 0.5f
				);
				verts.add(point);
				
				//Keep track of creases for later when deciding on even vert modification rule.
				verts.get(ed.getVertex0()).creaseVerts.add(verts.get(ed.getVertex1()));
				verts.get(ed.getVertex1()).creaseVerts.add(verts.get(ed.getVertex0()));
			}
				
			else {
				//Not a crease, so need to find the other verts on the two adjacent polygons
				
				ArrayList<Integer> leftEdges = edgeDS.getOtherEdgesOfLeftFace(i);
				ArrayList<Integer> rightEdges = edgeDS.getOtherEdgesOfRightFace(i);
				
				if (leftEdges.size() != 3 || rightEdges.size() != 3) {
					//Boundary (or incorrect data). Treat like a crease.
					Vert point = new Vert(
						(v1.x + v2.x) * 0.5f,
						(v1.y + v2.y) * 0.5f,
						(v1.z + v2.z) * 0.5f,
						(v1t.x + v2t.x) * 0.5f,
						(v1t.y + v2t.y) * 0.5f
					);
					verts.add(point);
				}
				else {
					//Three non-this edges per polygon, so proper quads. Average properly.
					PolygonData pd1 = edgeDS.getPolygonData(ed.getPolys().get(0));
					PolygonData pd2 = edgeDS.getPolygonData(ed.getPolys().get(1));
					
					Point3f pos = new Point3f();
					Point2f tex = new Point2f();
					Point3f v;
					
					//Put in the 1/16-weighted verts...
					for (Integer j: pd1.getAllVertices()) {
						if (j != ed.getVertex0() && j != ed.getVertex1()) {
							v = (Point3f)edgeDS.getVertexData(j).mData.getPosition().clone();
							v.scale(0.0625f);
							pos.add(v);
						}
					}
					
					for (Integer j: pd2.getAllVertices()) {
						if (j != ed.getVertex0() && j != ed.getVertex1()) {
							v = (Point3f)edgeDS.getVertexData(j).mData.getPosition().clone();
							v.scale(0.0625f);
							pos.add(v);
						}
					}
					
					//And put in the 3/8-weighted verts...
					v = new Point3f(v1);
					v.add(v2);
					v.scale(0.375f);
					pos.add(v);
					
					//And also do simple texture average along edges.
					tex.add(v1t);
					tex.add(v2t);
					tex.scale(0.5f);
					
					Vert point = new Vert(pos.x, pos.y, pos.z, tex.x, tex.y);
					
					verts.add(point);
				}
			}
			
			ed.setVertexIDNew(verts.size() - 1);
			
			//Save the new odd edge verts to the old even vert for later reference.
			verts.get(ed.getVertex0()).oddEdgeVerts.add(verts.get(verts.size() - 1));
			verts.get(ed.getVertex1()).oddEdgeVerts.add(verts.get(verts.size() - 1));
		}
		
		//Then create new even verts (move the even verts).
		for (Integer i: vertIDs) {
			Vert vd = verts.get(i);
			
			if (vd.creaseVerts.size() < 2) {
				//use smooth rule.
				
				int k = vd.oddEdgeVerts.size(); 
				float gamma = 1.0f / (4.0f * (float)k);
				float beta = 3.0f / (2.0f * (float)k);
				
				vd.x *= (1.0f - beta - gamma);
				vd.y *= (1.0f - beta - gamma);
				vd.z *= (1.0f - beta - gamma);
			
				for (Vert v : vd.oddEdgeVerts) {
					vd.x += v.x * beta / (float)k;
					vd.y += v.y * beta / (float)k;
					vd.z += v.z * beta / (float)k;
				}
				for (Vert v: vd.oddFaceVerts) {
					vd.x += v.x * gamma / (float)k;
					vd.y += v.y * gamma / (float)k;
					vd.z += v.z * gamma / (float)k;
				}
			}
			else if (vd.creaseVerts.size() == 2) {
				//use boundary rule.
				vd.x = vd.x * 0.75f + (vd.creaseVerts.get(0).x + vd.creaseVerts.get(1).x) * 0.125f;
				vd.y = vd.y * 0.75f + (vd.creaseVerts.get(0).y + vd.creaseVerts.get(1).y) * 0.125f;
				vd.z = vd.z * 0.75f + (vd.creaseVerts.get(0).z + vd.creaseVerts.get(1).z) * 0.125f;
			}
			else {
				//vert remains same.
			}
		}
		
		//Now create new crease and polygon lists connecting the new verts in the correct topology.
		
		//Add two creases for each old crease.
		for (Integer i: edgeIDs) {
			EdgeData ed = edgeDS.getEdgeData(i);
			
			if (edgeDS.isCreaseEdge(i)) {
				edges.add(new Edge(ed.getVertex0(), ed.getNewVertexID()));
				edges.add(new Edge(ed.getNewVertexID(), ed.getVertex1()));
			}
		}
		
		//Add four polygons each old polygon.
		for (Integer i: polygonIDs ) {
			PolygonData pd = edgeDS.getPolygonData(i);
			ArrayList<Integer> pded = pd.getAllEdges();
			ArrayList<Integer> pdvd = pd.getAllVertices();
			
			//even vertices (the variable numbers nonwithstanding...)
			int v1 = pdvd.get(0);
			int v3 = pdvd.get(1);
			int v5 = pdvd.get(2);
			int v7 = pdvd.get(3);
			
			//odd edge vertices
			int v2 = edgeDS.getEdgeData(pded.get(0)).getNewVertexID();
			int v4 = edgeDS.getEdgeData(pded.get(1)).getNewVertexID();
			int v6 = edgeDS.getEdgeData(pded.get(2)).getNewVertexID();
			int v8 = edgeDS.getEdgeData(pded.get(3)).getNewVertexID();
			
			//odd face vertex
			int v9 = pd.getNewFaceVertexID(); 
			
			//add new polys
			polys.add(new Poly(v1, v2, v9, v8));
			polys.add(new Poly(v3, v4, v9, v2));
			polys.add(new Poly(v5, v6, v9, v4));
			polys.add(new Poly(v7, v8, v9, v6));
		}
		
		
		//Copy verts to a single float array with format (x1, y1, z1, x2, y2, z2, ...)
		float[] vertArr = new float[verts.size() * 3];
		for (int i = 0; i < verts.size(); i++) {
			vertArr[i * 3 + 0] = verts.get(i).x;
			vertArr[i * 3 + 1] = verts.get(i).y;
			vertArr[i * 3 + 2] = verts.get(i).z;
		}
		
		//Copy vert texture coordinates into a single float array with format (u1, v1, u2, v2, ...)
		float[] texArr = new float[verts.size() * 2]; 
		for (int i = 0; i < verts.size(); i++) {
			texArr[i * 2 + 0] = verts.get(i).u;
			texArr[i * 2 + 1] = verts.get(i).v;
		}
		
		//Copy edges to an edge array with format (v11, v21, v12, v22, v13, v23, ...)
		int[] edgeArr = new int[edges.size() * 2];
		for (int i = 0; i < edges.size(); i++) {
			edgeArr[i * 2 + 0] = edges.get(i).v1;
			edgeArr[i * 2 + 1] = edges.get(i).v2;
		}
		
		//Copy polygons to a polygon array with format (v11, v21, v31, v41, v12, v22, v32, v42, ...)
		int[] polyArr = new int[polys.size() * 4];
		for (int i = 0; i < polys.size(); i++) {
			polyArr[i * 4 + 0] = polys.get(i).v1;
			polyArr[i * 4 + 1] = polys.get(i).v2;
			polyArr[i * 4 + 2] = polys.get(i).v3;
			polyArr[i * 4 + 3] = polys.get(i).v4;
		}
		
		//Finally create a new mesh.
		this.mMesh = new Quadmesh();
		
		//Place the verts in the new mesh...
		this.mMesh.setVertexData(FloatBuffer.wrap(vertArr));
		
		//Place the vert texture coords in the new mesh...
		this.mMesh.setTexCoordData(FloatBuffer.wrap(texArr));
		
		//Place the edges in the new mesh...
		this.mMesh.setEdgeData(IntBuffer.wrap(edgeArr));
		
		//Place the polygons in the new mesh...
		this.mMesh.setPolygonData(IntBuffer.wrap(polyArr));
		
		//And done!
	}
	
	public Mesh getNewMesh()
	{
		return this.mMesh;
	}
	
	public class Vert {
		public float x, y, z, u, v;
		public ArrayList<Vert> oddEdgeVerts;
		public ArrayList<Vert> oddFaceVerts;
		public ArrayList<Vert> creaseVerts;
		
		public Vert(float x, float y, float z, float u, float v) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.u = u;
			this.v = v;
			
			this.oddEdgeVerts = new ArrayList<Vert>(4);
			this.oddFaceVerts = new ArrayList<Vert>(4);
			this.creaseVerts = new ArrayList<Vert>(0);
		}
	}

	public class Edge {
		public int v1, v2;
		
		public Edge(int v1, int v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
	}
	
	public class Poly {
		public int v1, v2, v3, v4;
		
		public Poly (int v1, int v2, int v3, int v4) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.v4 = v4;
		}
	}
}
