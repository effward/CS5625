package cs5625.deferred.catmullclark;

import cs5625.deferred.datastruct.EdgeDS;
import cs5625.deferred.scenegraph.Mesh;

public class CCSubdiv {
	
	private Mesh mMesh;
	
	public CCSubdiv(EdgeDS edgeDS)
	{
		// TODO PA5: Fill in this function to perform catmull clark subdivision
		this.mMesh = edgeDS.getMesh();
		
		
		
	}
	
	public Mesh getNewMesh()
	{
		return this.mMesh;
	}
	
}
