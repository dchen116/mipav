package gov.nih.mipav.view.renderer.WildMagic.WormUntwisting;

import java.io.File;
import java.util.Vector;

import WildMagic.LibFoundation.Containment.ContBox3f;
import WildMagic.LibFoundation.Mathematics.Box3f;
import WildMagic.LibFoundation.Mathematics.Matrix3f;
import WildMagic.LibFoundation.Mathematics.Vector3f;
import WildMagic.LibGraphics.SceneGraph.BoxBV;

import gov.nih.mipav.model.file.FileIO;
import gov.nih.mipav.model.structures.ModelImage;
import gov.nih.mipav.model.structures.ModelStorageBase;
import gov.nih.mipav.model.structures.VOI;
import gov.nih.mipav.model.structures.VOIContour;
import gov.nih.mipav.model.structures.VOIText;
import gov.nih.mipav.model.structures.VOIVector;
import gov.nih.mipav.view.ViewJFrameImage;
import gov.nih.mipav.view.dialogs.JDialogBase;

public class WormData
{
	public static float VoxelSize =  0.1625f;

	public static final String autoSeamCellSegmentationOutput = new String("seam_cells");
	public static final String editSeamCellOutput = new String("seam_cell_final");
	public static final String autoLatticeGenerationOutput = new String("lattice_");
	public static final String editLatticeOutput = new String("lattice_final");
	public static final String editAnnotationInput = new String("annotation");
	public static final String editAnnotationOutput = new String("annotation_final");
	public static final String outputImages = new String("output_images");
	public static final String straightenedLattice = new String("straightened_lattice");
	public static final String straightenedAnnotations = new String("straightened_annotations");

	private static final int minPairDist = 5;
	private static final int maxPairDist = 15;		
	private static final float tenMinDist = 1;
	private static final float tenMaxDist = 5;  
	private static final float noseP1MinDist = 10;
	private static final float noseP1MaxDist = 30;
	private static final int minSequenceMidDist = 4;
	private static final int maxSequenceMidDist = 30;
	private static final int minSequenceDist = 4;
	private static final int maxSequenceDist = 25;
	private static final int sequenceDistDiffLimit = 12;
	private static final double sequenceTwistLimit = (Math.PI/2f);
	private static final int wormLengthMin = 100;
	private static final int wormLengthMax = 140;
	private static final int sequenceBendMin = 6;
	private static final int sequenceBendMax = 12;
	private static final int sequenceTimeLimit = 1000;
	
	private String imageName;
	private ModelImage wormImage;
	private ModelImage nucleiImage;
	private ModelImage seamSegmentation;
	private ModelImage skinSegmentation;
	private ModelImage insideSegmentation;
	private ModelImage nucleiSegmentation;

	private Vector<Vector3f> seamCellPoints;
	private VOI seamAnnotations;
	private VOIVector autoLattice;
	private float minSeamCellSegmentationIntensity = -1;
	
	private String outputDirectory = null;
	private String outputImagesDirectory = null;
	
	private int minSeamRadius = 8;
	private int maxSeamRadius = 25;
	
	public WormData( ModelImage image )
	{
		wormImage = image;

		if ( wormImage != null )
		{
			imageName = wormImage.getImageName();
			if (imageName.contains("_clone")) {
				imageName = imageName.replaceAll("_clone", "");
			}
			outputDirectory = new String(wormImage.getImageDirectory() + JDialogBase.makeImageName(imageName, "") + File.separator + JDialogBase.makeImageName(imageName, "_results") );
			outputImagesDirectory = outputDirectory + File.separator + "output_images" + File.separator;
			String parentDir = new String(wormImage.getImageDirectory() + JDialogBase.makeImageName(imageName, "") + File.separator);
			checkParentDir(parentDir);
			File dir = new File(outputDirectory);
			if ( !dir.exists() )
			{
				dir.mkdir();
			}
			dir = new File(outputImagesDirectory);
			if ( !dir.exists() )
			{
				dir.mkdir();
			}
			
			float voxelResolution = wormImage.getResolutions(0)[0];
			if ( voxelResolution != VoxelSize )
			{
				VoxelSize = voxelResolution;
			}
			minSeamCellSegmentationIntensity = (float) (0.1 * wormImage.getMin());
//			System.err.println( minSeamCellSegmentationIntensity );
		}
	}
	
	public void dispose()
	{
		if ( nucleiSegmentation != null )
		{
			nucleiSegmentation.disposeLocal(false);
			nucleiSegmentation = null;
		}
		if ( skinSegmentation != null )
		{
			skinSegmentation.disposeLocal(false);
			skinSegmentation = null;
		}
		if ( insideSegmentation != null )
		{
			insideSegmentation.disposeLocal(false);
			insideSegmentation = null;
		}
		if ( seamSegmentation != null )
		{
			seamSegmentation.disposeLocal(false);
			seamSegmentation = null;
		}
	}

	/**
	 * Creates the parent directory for the output images and data created by worm segmentation and untwisting:
	 * @param parentDir
	 */
	private void checkParentDir( String parentDir )
	{
		File parentFileDir = new File(parentDir);
		if (parentFileDir.exists() && parentFileDir.isDirectory()) { // do nothing
		} else if (parentFileDir.exists() && !parentFileDir.isDirectory()) { // do nothing
		} else { // parentDir does not exist
			parentFileDir.mkdir();
		}
	}
	
	public VOI getSeamAnnotations()
	{
		return seamAnnotations;
	}
	
	public void saveSeamAnnotations()
	{
		seamCellPoints = new Vector<Vector3f>();
		for ( int i = 0; i < seamAnnotations.getCurves().size(); i++ )
		{
			VOIText text = (VOIText)seamAnnotations.getCurves().elementAt(i);
			seamCellPoints.add(new Vector3f(text.elementAt(0)));
		}
		seamAnnotations.setName("seam cells");
		LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + editSeamCellOutput + File.separator, wormImage);
	}
	
	public void saveLattice()
	{
		LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + editLatticeOutput + File.separator, wormImage);
	}
	
	public Vector<Vector3f> readSeamCells()
	{	
		VOIVector annotationVector = new VOIVector();
		LatticeModel.loadAllVOIsFrom(wormImage, outputDirectory + File.separator + editSeamCellOutput + File.separator, true, annotationVector, false);
		if ( annotationVector.size() > 0 )
		{
			seamAnnotations = annotationVector.elementAt(0);
		}
		if ( seamAnnotations == null )
		{
			LatticeModel.loadAllVOIsFrom(wormImage, outputDirectory + File.separator + autoSeamCellSegmentationOutput + File.separator, true, annotationVector, false);
			if ( annotationVector.size() > 0 )
			{
				seamAnnotations = annotationVector.elementAt(0);
			}
			if ( seamAnnotations == null )
			{
				return null;
			}
		}
		seamCellPoints = new Vector<Vector3f>();
		for ( int i = 0; i < seamAnnotations.getCurves().size(); i++ )
		{
			VOIText text = (VOIText)seamAnnotations.getCurves().elementAt(i);
			seamCellPoints.add(new Vector3f(text.elementAt(0)));
		}
		return seamCellPoints;
	}

	public void segmentSeamCells(int minRadius, int maxRadius)
	{
		minSeamRadius = minRadius;
		maxSeamRadius = minRadius;
		//		System.err.println( "   segmentSeamCells: start" );
		if (seamCellPoints == null)
		{
			String seamCellDir = outputImagesDirectory + File.separator;
			File outputDir = new File(seamCellDir);
			if ( !outputDir.exists() )
			{
				outputDir.mkdir();
			}
			seamCellPoints = new Vector<Vector3f>();
			VOI finalClusterList = new VOI((short)0, "temp", VOI.POLYLINE, 0);

			seamSegmentation = new ModelImage( ModelStorageBase.FLOAT, wormImage.getExtents(), "seamCellImage" );	
			seamSegmentation.setImageName("seamCellImage");
			JDialogBase.updateFileInfo(wormImage, seamSegmentation);
			
			WormSegmentation segmentation = new WormSegmentation();
			boolean[] passTest = new boolean[6];
			VOIContour[] ptsTest = new VOIContour[6];
			VOI[] clusterTest = new VOI[6];
			
			int testCount = 0;
			ptsTest[testCount] = new VOIContour(false);
			clusterTest[testCount] = new VOI((short)0, "temp", VOI.POLYLINE, 0);			
			passTest[testCount] = segmentation.segmentSeamNew( wormImage, ptsTest[0], clusterTest[0], minRadius, maxRadius, outputImagesDirectory, 22, true );

			//			VOI seamTemp = new VOI((short) 0, "seam cells", VOI.ANNOTATION, -1.0f);
			//			wormImage.unregisterAllVOIs();
			//			wormImage.registerVOI(seamTemp);
			//			for ( int i = 0; i < pts1.size(); i++ )
			//			{
			//				//			System.err.println( i );
			//				VOIText text = new VOIText();
			//				text.add(pts1.elementAt(i));
			//				text.add(pts1.elementAt(i));
			//				text.setText( "" + (i+1) );
			//				text.setUseMarker(false);
			//				text.update();
			//
			//				//			int x = (int)seamCellPoints.elementAt(i).X;
			//				//			int y = (int)seamCellPoints.elementAt(i).Y;
			//				//			int z = (int)seamCellPoints.elementAt(i).Z;
			//				//			System.err.println( "Seam Cell " + (i+1) + "  " + wormImage.getFloat(x,y,z) );
			//				seamTemp.getCurves().add(text);
			//			}
			//			LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + "seam_test1" + File.separator, wormImage);



			testCount++;
			ptsTest[testCount] = new VOIContour(false);
			clusterTest[testCount] = new VOI((short)0, "temp", VOI.POLYLINE, 0);
			passTest[testCount] = segmentation.segmentSeamNew( wormImage, ptsTest[1], clusterTest[1], minRadius-2, maxRadius, outputImagesDirectory, 22, true );

			//			seamTemp = new VOI((short) 0, "seam cells", VOI.ANNOTATION, -1.0f);
			//			wormImage.unregisterAllVOIs();
			//			wormImage.registerVOI(seamTemp);
			//			for ( int i = 0; i < pts1a.size(); i++ )
			//			{
			//				//			System.err.println( i );
			//				VOIText text = new VOIText();
			//				text.add(pts1a.elementAt(i));
			//				text.add(pts1a.elementAt(i));
			//				text.setText( "" + (i+1) );
			//				text.setUseMarker(false);
			//				text.update();
			//
			//				//			int x = (int)seamCellPoints.elementAt(i).X;
			//				//			int y = (int)seamCellPoints.elementAt(i).Y;
			//				//			int z = (int)seamCellPoints.elementAt(i).Z;
			//				//			System.err.println( "Seam Cell " + (i+1) + "  " + wormImage.getFloat(x,y,z) );
			//				seamTemp.getCurves().add(text);
			//			}
			//			LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + "seam_test2" + File.separator, wormImage);






			boolean found = false;
			if ( passTest[0] && passTest[1] && allMatch(ptsTest[0], clusterTest[0], ptsTest[1], clusterTest[1], minRadius, seamCellPoints, finalClusterList ) )
			{
				found = true;
			}

			if ( !found )
			{
				testCount++;
				ptsTest[testCount] = new VOIContour(false);
				clusterTest[testCount] = new VOI((short)0, "temp", VOI.POLYLINE, 0);
				passTest[testCount] = segmentation.segmentSeamNew( wormImage, ptsTest[2], clusterTest[2], minRadius, maxRadius, outputImagesDirectory, 20, true );
				//				seamTemp = new VOI((short) 0, "seam cells", VOI.ANNOTATION, -1.0f);
				//				wormImage.unregisterAllVOIs();
				//				wormImage.registerVOI(seamTemp);
				//				for ( int i = 0; i < pts2.size(); i++ )
				//				{
				//					//			System.err.println( i );
				//					VOIText text = new VOIText();
				//					text.add(pts2.elementAt(i));
				//					text.add(pts2.elementAt(i));
				//					text.setText( "" + (i+1) );
				//					text.setUseMarker(false);
				//					text.update();
				//
				//					//			int x = (int)seamCellPoints.elementAt(i).X;
				//					//			int y = (int)seamCellPoints.elementAt(i).Y;
				//					//			int z = (int)seamCellPoints.elementAt(i).Z;
				//					//			System.err.println( "Seam Cell " + (i+1) + "  " + wormImage.getFloat(x,y,z) );
				//					seamTemp.getCurves().add(text);
				//				}
				//				LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + "seam_test3" + File.separator, wormImage);
				for ( int i = 0; i <= testCount; i++ )
				{
					for ( int j = i+1; j <= testCount; j++ )
					{
						if ( passTest[i] && passTest[j] && allMatch(ptsTest[i], clusterTest[1], ptsTest[j], clusterTest[i], minRadius, seamCellPoints, finalClusterList) )
						{
							found = true;
							break;
						}
					}
					if ( found )
					{
						break;
					}
				}
				
				if ( !found )
				{
					testCount++;
					ptsTest[testCount] = new VOIContour(false);
					clusterTest[testCount] = new VOI((short)0, "temp", VOI.POLYLINE, 0);
					passTest[testCount] = segmentation.segmentSeamNew( wormImage, ptsTest[3], clusterTest[3], minRadius-2, maxRadius, outputImagesDirectory, 20, true );
					//					seamTemp = new VOI((short) 0, "seam cells", VOI.ANNOTATION, -1.0f);
					//					wormImage.unregisterAllVOIs();
					//					wormImage.registerVOI(seamTemp);
					//					for ( int i = 0; i < pts2a.size(); i++ )
					//					{
					//						//			System.err.println( i );
					//						VOIText text = new VOIText();
					//						text.add(pts2a.elementAt(i));
					//						text.add(pts2a.elementAt(i));
					//						text.setText( "" + (i+1) );
					//						text.setUseMarker(false);
					//						text.update();
					//
					//						//			int x = (int)seamCellPoints.elementAt(i).X;
					//						//			int y = (int)seamCellPoints.elementAt(i).Y;
					//						//			int z = (int)seamCellPoints.elementAt(i).Z;
					//						//			System.err.println( "Seam Cell " + (i+1) + "  " + wormImage.getFloat(x,y,z) );
					//						seamTemp.getCurves().add(text);
					//					}
					//					LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + "seam_test4" + File.separator, wormImage);

					for ( int i = 0; i <= testCount; i++ )
					{
						for ( int j = i+1; j <= testCount; j++ )
						{
							if ( passTest[i] && passTest[j] && allMatch(ptsTest[i], clusterTest[1], ptsTest[j], clusterTest[i], minRadius, seamCellPoints, finalClusterList) )
							{
								found = true;
								break;
							}
						}
						if ( found )
						{
							break;
						}
					}


					if ( !found )
					{
						testCount++;
						ptsTest[testCount] = new VOIContour(false);
						clusterTest[testCount] = new VOI((short)0, "temp", VOI.POLYLINE, 0);
						passTest[testCount] = segmentation.segmentSeamNew( wormImage, ptsTest[4], clusterTest[4], minRadius, maxRadius, outputImagesDirectory, 18, false );
						for ( int i = 0; i <= testCount; i++ )
						{
							for ( int j = i+1; j <= testCount; j++ )
							{
								if ( passTest[i] && passTest[j] && allMatch(ptsTest[i], clusterTest[1], ptsTest[j], clusterTest[i], minRadius, seamCellPoints, finalClusterList) )
								{
									found = true;
									break;
								}
							}
							if ( found )
							{
								break;
							}
						}

						if ( !found )
						{
							testCount++;
							ptsTest[testCount] = new VOIContour(false);
							clusterTest[testCount] = new VOI((short)0, "temp", VOI.POLYLINE, 0);
							passTest[testCount] = segmentation.segmentSeamNew( wormImage, ptsTest[4], clusterTest[4], minRadius-2, maxRadius, outputImagesDirectory, 18, false );
							for ( int i = 0; i <= testCount; i++ )
							{
								for ( int j = i+1; j <= testCount; j++ )
								{
									if ( passTest[i] && passTest[j] && allMatch(ptsTest[i], clusterTest[1], ptsTest[j], clusterTest[i], minRadius, seamCellPoints, finalClusterList) )
									{
										found = true;
										break;
									}
								}
								if ( found )
								{
									break;
								}
							}
						}
					}
				}
			}
			
			
			
			System.err.println( found + "   " + seamCellPoints.size() );
			if ( found )
			{
				int clusterCount = 1;
				for ( int i = 0; i < seamSegmentation.getDataSize(); i++ )
				{
					seamSegmentation.set(i, 0);
				}
				for ( int i = 0; i < finalClusterList.getCurves().size(); i++ )
				{
					VOIContour cluster = (VOIContour) finalClusterList.getCurves().elementAt(i);
					for ( int j = 0; j < cluster.size(); j++ )
					{
						int x = Math.round(cluster.elementAt(j).X);
						int y = Math.round(cluster.elementAt(j).Y);
						int z = Math.round(cluster.elementAt(j).Z);

						seamSegmentation.set(x, y, z, clusterCount);
						float value = wormImage.getFloat(x,y,z);
						if ( value < minSeamCellSegmentationIntensity )
						{
							minSeamCellSegmentationIntensity = value;
						}
					}
					clusterCount++;
				}
				seamSegmentation.setImageName("seamCellImage");
				JDialogBase.updateFileInfo(wormImage, seamSegmentation);
				ModelImage.saveImage(seamSegmentation, seamSegmentation.getImageName() + ".xml", outputImagesDirectory, false);
			}
			
//			minSeamCellSegmentationIntensity = segmentation.getMinSegmentationIntensity();
		}
		if ( seamAnnotations == null )
		{
			seamAnnotations = new VOI((short) 0, "seam cells", VOI.ANNOTATION, -1.0f);
			wormImage.registerVOI(seamAnnotations);
			for ( int i = 0; i < seamCellPoints.size(); i++ )
			{
				//			System.err.println( i );
				VOIText text = new VOIText();
				text.add(seamCellPoints.elementAt(i));
				text.add(seamCellPoints.elementAt(i));
				text.setText( "" + (i+1) );
				text.setUseMarker(false);
				text.update();

				//			int x = (int)seamCellPoints.elementAt(i).X;
				//			int y = (int)seamCellPoints.elementAt(i).Y;
				//			int z = (int)seamCellPoints.elementAt(i).Z;
				//			System.err.println( "Seam Cell " + (i+1) + "  " + wormImage.getFloat(x,y,z) );
				seamAnnotations.getCurves().add(text);
			}

			LatticeModel.saveAllVOIsTo(outputDirectory + File.separator + autoSeamCellSegmentationOutput + File.separator, wormImage);
		}
//		System.err.println( "   segmentSeamCells: end " + minSeamCellSegmentationIntensity );
	}
	
	public void setNucleiImage(ModelImage image)
	{
//		nucleiImage = image;
//		if ( nucleiBlur == null )
//		{
//			nucleiBlur = WormSegmentation.blur(nucleiImage, 3);
//		}
//		if ( wormBlur != null )
//		{
//			wormBlur = WormSegmentation.blur(wormImage, 3);
//		}
//		float maxNuclei = (float) nucleiBlur.getMax();
//		float maxWorm = (float) wormBlur.getMax();
//
//		ModelImage insideOut = new ModelImage(ModelStorageBase.FLOAT, nucleiImage.getExtents(), "insideOut.tif");
//		float min = Float.MAX_VALUE;
//		float max = -Float.MAX_VALUE;
//		for ( int i = 0; i < nucleiImage.getDataSize(); i++ )
//		{
//			float inside = insideSegmentation.getFloat(i);
//			if ( inside > 0 )
//			{
//				float wormValue = wormBlur.getFloat(i);
//				float nucleiValue = nucleiBlur.getFloat(i);
//				if ( (wormValue > minSeamCellSegmentationIntensity) && (nucleiValue < .1*maxNuclei) )
//				{
//					insideOut.set(i, 10);
//				}
//			}
//		}
//		insideOut.setMax(10);
//		insideOut.setMin(0);
//		new ViewJFrameImage(insideOut);
	}
	
	public Vector<Vector3f> getSeamCells()
	{
		return seamCellPoints;
	}
	
	public ModelImage getSeamSegmentation()
	{
		return seamSegmentation;
	}
	
	public ModelImage readSeamSegmentation()
	{
		String seamCellDir = outputImagesDirectory + File.separator;
		File inputFile = new File(seamCellDir + "seamCellImage.xml");
		if ( inputFile.exists() )
		{
			FileIO fileIO = new FileIO();
			seamSegmentation = fileIO.readImage( seamCellDir + "seamCellImage.xml" );
		} 
		float minSeam = Float.MAX_VALUE;
		for ( int i = 0; i < seamSegmentation.getDataSize(); i++ )
		{
			if ( seamSegmentation.getFloat(i) > 0 )
			{
				float value = wormImage.getFloat(i);
				if ( value < minSeam )
				{
					minSeam = value;
				}
			}
		}
		if ( minSeam != Float.MAX_VALUE )
		{
			minSeamCellSegmentationIntensity = minSeam;
		}
		return seamSegmentation;
	}
	
	public ModelImage readSkinSegmentation()
	{
		String seamCellDir = outputImagesDirectory + File.separator;
		File inputFile = new File(seamCellDir + "skinImage.xml");
		if ( inputFile.exists() )
		{
			FileIO fileIO = new FileIO();
			skinSegmentation = fileIO.readImage( seamCellDir + "skinImage.xml" );
		} 
		return skinSegmentation;
	}
	
	public void segmentSkin()
	{
		if ( skinSegmentation == null )
		{
			ModelImage wormBlur = WormSegmentation.blur(wormImage, 3);

//			ModelImage surface = WormSegmentation.outlineA( wormBlur, minSeamCellSegmentationIntensity );
//			ModelImage surface = new ModelImage( ModelStorageBase.ARGB_FLOAT, wormImage.getExtents(), imageName + "_skin" );
//			WormSegmentation.outline2( outputImagesDirectory, wormImage, surface, minSeamCellSegmentationIntensity);
			
			ModelImage[] outsideInside = WormSegmentation.outside(wormBlur, 5);
			skinSegmentation = outsideInside[0];
			insideSegmentation = outsideInside[1];
			skinSegmentation.setImageName( "skinImage" );
			ModelImage.saveImage(skinSegmentation, skinSegmentation.getImageName() + ".xml", outputImagesDirectory, false);
			
//			skinSegmentation.calcMinMax();
//			new ViewJFrameImage((ModelImage)skinSegmentation.clone());
//			
//			insideSegmentation.calcMinMax();
//			new ViewJFrameImage((ModelImage)insideSegmentation.clone());
			
//			if ( seamCellPoints != null )
//			{
//				// scan skinSegmentation image for nearest skin pt to each seam cell point...
//				int dimX = skinSegmentation.getExtents().length > 0 ? skinSegmentation.getExtents()[0] : 1;
//				int dimY = skinSegmentation.getExtents().length > 1 ? skinSegmentation.getExtents()[1] : 1;
//				int dimZ = skinSegmentation.getExtents().length > 2 ? skinSegmentation.getExtents()[2] : 1;
//
//				Vector<Vector3f> nearest = new Vector<Vector3f>();
//				Vector<Float>  distances = new Vector<Float>();
//				for ( int i = 0; i < seamCellPoints.size(); i++ )
//				{
//					nearest.add(new Vector3f());
//					distances.add( nearest.elementAt(i).distance(seamCellPoints.elementAt(i)));
//				}
//				Vector3f temp = new Vector3f();
//				for ( int z = 0; z < dimZ; z++ )
//				{
//					for ( int y = 0; y < dimY; y++ )
//					{
//						for ( int x = 0; x < dimX; x++ )
//						{
//							if ( skinSegmentation.getFloat(x, y, z ) > 0 )
//							{
//								temp.set(x, y, z);
//								for ( int i = 0; i < seamCellPoints.size(); i++ )
//								{
//									if ( temp.distance(seamCellPoints.elementAt(i)) < distances.elementAt(i) )
//									{
//										nearest.elementAt(i).copy(temp);
//										distances.remove(i);
//										distances.add(i, temp.distance(seamCellPoints.elementAt(i)));
//									}
//								}
//							}
//						}
//					}
//				}
//				for ( int i = 0; i < seamCellPoints.size(); i++ )
//				{
////					System.err.println( i + " " + distances.elementAt(i) );
////					if ( distances.elementAt(i) < 30 )
////					{
//						seamAnnotations.getCurves().elementAt(i).remove(1);
//						seamAnnotations.getCurves().elementAt(i).add(nearest.elementAt(i));
//						((VOIText)seamAnnotations.getCurves().elementAt(i)).setUseMarker(true);
//						seamAnnotations.getCurves().elementAt(i).update();
////					}
//				}
//			}
		}
	}
	
	public void testLattice( Vector<Vector3f> left, Vector<Vector3f> right )
	{
		if ( seamSegmentation == null )
		{
			readSeamCells();
			readSeamSegmentation();
		}
		if ( seamCellPoints == null )
		{
			segmentSeamCells(8,25);
		}
		if ( skinSegmentation == null )
		{
			segmentSkin();
		}
		LatticeBuilder builder = new LatticeBuilder();
		builder.setSeamImage(seamSegmentation);
		builder.setSkinImage(skinSegmentation);

		Vector3f[] leftTemp = new Vector3f[left.size()];
		Vector3f[] rightTemp = new Vector3f[right.size()];
		for ( int i = 0; i < left.size(); i++ )
		{
			leftTemp[i] = left.elementAt((left.size()-1)-i);
			rightTemp[i] = right.elementAt((right.size()-1)-i);
		}
		left.removeAllElements();
		right.removeAllElements();
		for ( int i = 0; i < leftTemp.length; i++ )
		{
			left.add(leftTemp[i]);
			right.add(rightTemp[i]);
		}
		System.err.println( "lattice test " + builder.testLattice(wormImage, left, right) );
	}
	
	public int generateLattice()
	{
		if ( seamSegmentation == null )
		{
			readSeamSegmentation();
			if ( seamSegmentation == null )
			{
				segmentSeamCells(minSeamRadius, maxSeamRadius);
			}
		}
		if ( seamAnnotations == null )
		{
			readSeamCells();
			if ( seamAnnotations == null )
			{
				segmentSeamCells(minSeamRadius, maxSeamRadius);
			}
		}
		if ( skinSegmentation == null )
		{
			readSkinSegmentation();
			if ( skinSegmentation == null )
			{
				segmentSkin();
			}
		}
		LatticeBuilder builder = new LatticeBuilder();
		builder.setSeamImage(seamSegmentation);
		builder.setSkinImage(skinSegmentation);
		// build the lattices from input image and list of points:
		autoLattice = builder.buildLattice( null, 0, 0, wormImage, seamAnnotations, minSeamCellSegmentationIntensity, null, outputDirectory );
		return autoLattice.size();
	}
	
	public VOIVector getAutoLattice()
	{
		return autoLattice;
	}
	
	public VOIVector[] readAutoLattice()
	{
		VOIVector[] latticeList = new VOIVector[5];
		// read top 5 lattices:
		for ( int i = 0; i < 5; i++ )
		{
			String fileName = outputDirectory + File.separator + autoLatticeGenerationOutput + (i+1) + File.separator;
			File outputFileDir = new File(fileName);
			if ( outputFileDir.exists() )
			{
				latticeList[i] = new VOIVector();
				LatticeModel.loadAllVOIsFrom(wormImage, outputDirectory + File.separator + autoLatticeGenerationOutput + (i+1) + File.separator, true, latticeList[i], false);
			}
		}
		return latticeList;
	}

	private float surfaceCount( ModelImage skinImage, Vector3f pt1, Vector3f pt2 )
	{
		Vector3f dir = Vector3f.sub(pt2, pt1);
		float length = dir.normalize();
		
		Vector3f center = Vector3f.add(pt1, pt2);
		center.scale(0.5f);

		Vector3f basisVectorX = new Vector3f(1, 0, 0);
		Vector3f basisVectorY = new Vector3f(0, 1, 0);
		Vector3f basisVectorZ = new Vector3f(0, 0, 1);
		Vector3f rotationAxis = Vector3f.cross(basisVectorZ, dir);
		rotationAxis.normalize();
		float angle = basisVectorZ.angle(dir);
		Matrix3f mat = new Matrix3f(true);
		mat.fromAxisAngle(rotationAxis, angle);

		Vector3f rotatedX = mat.multRight(basisVectorX);
		Vector3f rotatedY = mat.multRight(basisVectorY);

		Box3f box = new Box3f( center, rotatedX, rotatedY, dir, 10, 10, length );

		int dimX = skinSegmentation.getExtents().length > 0 ? skinSegmentation.getExtents()[0] : 1;
		int dimY = skinSegmentation.getExtents().length > 1 ? skinSegmentation.getExtents()[1] : 1;
		int dimZ = skinSegmentation.getExtents().length > 2 ? skinSegmentation.getExtents()[2] : 1;
		float totalCount = 0;
		float skinCount = 0;
		Vector3f pt = new Vector3f();
		for ( int z = 0; z < dimZ; z++ )
		{
			for ( int y = 0; y < dimY; y++ )
			{
				for ( int x = 0; x < dimX; x++ )
				{
					pt.set(x, y, z);
					if ( ContBox3f.InBox( pt, box ) )
					{
						totalCount++;
						if ( skinSegmentation.getFloat(x, y, z ) > 0 )
						{
							skinCount++;
						}
					}
				}
			}
		}
		return (skinCount/totalCount);
	}
	
	private boolean allMatch(VOIContour pts1, VOI clusterList1, VOIContour pts2, VOI clusterList2, float minDist, Vector<Vector3f> finalPtsList, VOI finalClusterList )
	{
		if ( pts1.size() > pts2.size() )
		{
			return allMatch(pts2, clusterList2, pts1, clusterList1, minDist, finalPtsList, finalClusterList);
		}
		
		for ( int i = 0; i < pts1.size(); i++ )
		{
			boolean foundMatch = false;
			float minDistTest = Float.MAX_VALUE;
			for ( int j = 0; j < pts2.size(); j++ )
			{
				float distance = pts1.elementAt(i).distance(pts2.elementAt(j));
				if ( distance < minDistTest )
				{
					minDistTest = distance;
				}
				if ( distance < minDist )
				{
					foundMatch = true;
					break;
				}
			}
			if ( !foundMatch )
			{
				System.err.println( i + "   " + minDistTest + "   " + minDist );
				return false;
			}
		}

		// Add larger list:
		for ( int i = 0; i < pts2.size(); i++ )
		{
			finalPtsList.add( pts2.elementAt(i) );
			finalClusterList.getCurves().add( clusterList2.getCurves().elementAt(i) );
		}
		return true;
	}
	
}