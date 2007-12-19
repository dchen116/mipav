package gov.nih.mipav.view.renderer.WildMagic;


import gov.nih.mipav.view.WildMagic.LibFoundation.Mathematics.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.Effects.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.Rendering.*;
import gov.nih.mipav.view.WildMagic.LibGraphics.SceneGraph.*;

public class VolumeClip extends VolumeObject
{
    public VolumeClip ( VolumeImage kImageA, Vector3f kTranslate, float fX, float fY, float fZ )
    {
        super(kImageA,kTranslate,fX,fY,fZ);

        m_kVertexColor3Shader = new VertexColor3Effect();
        CreateClipPlanes();
        m_kScene.UpdateGS();
        m_kScene.UpdateRS();
    }

    public void PreRender( Renderer kRenderer, Culler kCuller )
    {
        if ( !m_bDisplay )
        {
            return;
        }
        m_kScene.UpdateGS();
        kCuller.ComputeVisibleSet(m_kScene);
        kRenderer.DrawScene(kCuller.GetVisibleSet());
    }

    public void Render( Renderer kRenderer, Culler kCuller )
    {
        if ( !m_bDisplay )
        {
            return;
        }
        m_kScene.UpdateGS();
        kCuller.ComputeVisibleSet(m_kScene);
        kRenderer.DrawScene(kCuller.GetVisibleSet());
    }

    public void PostRender( Renderer kRenderer, Culler kCuller )
    {
        if ( m_bDisplayClipEye || m_bDisplayClipEyeInv )
        {
            Camera kCamera = kRenderer.GetCamera();

            m_spkEyeCamera.SetLocation(kCamera.GetLocation());
            kRenderer.SetCamera(m_spkEyeCamera);
            if ( m_bDisplayClipEye )
            {
                kRenderer.Draw(m_kClipEye);
            }
            if ( m_bDisplayClipEyeInv )
            {
                kRenderer.Draw(m_kClipEyeInv);
            }
            kRenderer.SetCamera(kCamera);
        }
    }


    public void dispose()
    {
        if ( m_kArbitraryClip != null )
        {
            m_kArbitraryClip.dispose();
            m_kArbitraryClip = null;
        }
        if ( m_spkEyeCamera != null )
        {
            m_spkEyeCamera.dispose();
            m_spkEyeCamera = null;
        }
        for ( int i = 0; i < 6; i++ )
        {
            m_akPolyline[i].dispose();
            m_akPolyline[i] = null;
        }
        m_akPolyline = null;
    }

    /**
     * Called from JPanelClip. Sets the axis-aligned clip plane display on/off.
     * @param iWhich, the clip plane to set.
     * @param bDisplay on/off.
     */
    public void displayClipPlane( int iWhich, boolean bDisplay )
    {
        if ( bDisplay != m_abDisplayPolyline[iWhich] )
        {
            m_abDisplayPolyline[iWhich] = bDisplay;
            if ( bDisplay )
            {
                m_kScene.AttachChild(m_akPolyline[iWhich]);
            }
            else
            {
                m_kScene.DetachChild(m_akPolyline[iWhich]);
            }
        }
        m_kScene.UpdateGS();
        m_kScene.UpdateRS();
    }

    /**
     * Sets the axis-aligned clip plane color.
     * @param iWhich, one of the 6 clip planes
     * @param kColor, the new color.
     */
    public void setClipPlaneColor( int iWhich, ColorRGB kColor )
    {
        for ( int i = 0; i < 4; i++ )
        {
            m_akPolyline[iWhich].VBuffer.SetColor3( 0, i, kColor );
        }
        m_akPolyline[iWhich].VBuffer.Release();
    }

    /**
     * Sets the eye clip plane color.
     * @param kColor, the new color.
     */
    public void setEyeColor( ColorRGB kColor )
    {
        for ( int i = 0; i < 4; i++ )
        {
            m_kClipEye.VBuffer.SetColor3( 0, i, kColor );
        }
        m_kClipEye.VBuffer.Release();
    }

    /**
     * Sets the eye clip plane color.
     * @param kColor, the new color.
     */
    public void setEyeInvColor( ColorRGB kColor )
    {
        for ( int i = 0; i < 4; i++ )
        {
            m_kClipEyeInv.VBuffer.SetColor3( 0, i, kColor );
        }
        m_kClipEyeInv.VBuffer.Release();
    }


    public void setClipPlane( int iWhich, float fValue )
    {
        for ( int i = 0; i < 4; i++ )
        {
            if ( iWhich < 2 )
            {
                m_akPolyline[iWhich].VBuffer.SetPosition3(i, 
                                                          fValue*m_fX,
                                                          m_akPolyline[iWhich].VBuffer.GetPosition3fY(i),     
                                                          m_akPolyline[iWhich].VBuffer.GetPosition3fZ(i));
            }
            else if ( iWhich < 4 )
            {
                m_akPolyline[iWhich].VBuffer.SetPosition3(i, 
                                                          m_akPolyline[iWhich].VBuffer.GetPosition3fX(i),     
                                                          fValue*m_fY,
                                                          m_akPolyline[iWhich].VBuffer.GetPosition3fZ(i));
            }
            else
            {
                m_akPolyline[iWhich].VBuffer.SetPosition3(i, 
                                                          m_akPolyline[iWhich].VBuffer.GetPosition3fX(i),     
                                                          m_akPolyline[iWhich].VBuffer.GetPosition3fY(i),
                                                          fValue*m_fZ
                                                          );
            }
        }
        m_akPolyline[iWhich].VBuffer.Release();
        
        m_kScene.UpdateGS();
        m_kScene.UpdateRS();

    }


    /**
     * Sets the eye clip plane position.
     * @param f4 clip position (same value as sSlice in JPanelClip)
     * @param bDisplay on/off.
     */
    public void setEyeClipPlane( float fZ )
    {
        m_kClipEye.VBuffer.SetPosition3( 0, -.2f, -.2f, fZ ) ;
        m_kClipEye.VBuffer.SetPosition3( 1, m_fX +.2f, -.2f, fZ ) ;
        m_kClipEye.VBuffer.SetPosition3( 2, m_fX +.2f, m_fY +.2f, fZ ) ;
        m_kClipEye.VBuffer.SetPosition3( 3, -.2f, m_fY +.2f, fZ ) ;
        m_kClipEye.VBuffer.Release();

        m_kClipEye.UpdateGS();
        m_kClipEye.UpdateRS();
    }

    /**
     * Sets the eye clip plane position.
     * @param f4 clip position (same value as sSlice in JPanelClip)
     * @param bDisplay on/off.
     */
    public void setEyeInvClipPlane( float fZ )
    {
        m_kClipEyeInv.VBuffer.SetPosition3( 0, -.2f, -.2f, fZ ) ;
        m_kClipEyeInv.VBuffer.SetPosition3( 1, m_fX +.2f, -.2f, fZ ) ;
        m_kClipEyeInv.VBuffer.SetPosition3( 2, m_fX +.2f, m_fY +.2f, fZ ) ;
        m_kClipEyeInv.VBuffer.SetPosition3( 3, -.2f, m_fY +.2f, fZ );
        m_kClipEyeInv.VBuffer.Release();

        m_kClipEyeInv.UpdateGS();
        m_kClipEyeInv.UpdateRS();
    }

    public void DisplayEye(boolean bDisplay)
    {
        m_bDisplayClipEye = bDisplay;
    }

    public void DisplayEyeInv(boolean bDisplay)
    {
        m_bDisplayClipEyeInv = bDisplay;
    }


    public float GetValue(int iWhich)
    {
        float fValue = 0;
        if ( iWhich < 2 )
        {
            fValue = m_akPolyline[iWhich].VBuffer.GetPosition3fX( 0 );
            fValue /= m_fX;
        }
        else if ( iWhich < 4 )
        {
            fValue = m_akPolyline[iWhich].VBuffer.GetPosition3fY( 0 );
            fValue /= m_fY;
        }
        else
        {
            fValue = m_akPolyline[iWhich].VBuffer.GetPosition3fZ( 0 );
            fValue /= m_fZ;
        }
        return fValue;
    }


    private void CreateClipPlanes()
    {
        m_kScene = new Node();

        m_kCull = new CullState();
        m_kCull.Enabled = false;
        m_kScene.AttachGlobalState(m_kCull);

        m_kAlpha = new AlphaState();
        m_kAlpha.BlendEnabled = true;
        m_kScene.AttachGlobalState(m_kAlpha);


        m_akPolyline = new Polyline[6];
        IndexBuffer kIndexBuffer = new IndexBuffer(6);
        int[] aiIndexData = kIndexBuffer.GetData();
        aiIndexData[0] = 0;
        aiIndexData[1] = 1;
        aiIndexData[2] = 2;
        aiIndexData[3] = 0;
        aiIndexData[4] = 2;
        aiIndexData[5] = 3;

        Attributes kAttributes = new Attributes();
        kAttributes.SetPChannels(3);
        kAttributes.SetCChannels(0,3);

        VertexBuffer[] akOutlineSquare = new VertexBuffer[6];
        for ( int i = 0; i < 6; i++ )
        {
            akOutlineSquare[i] = new VertexBuffer(kAttributes, 4 );
            for ( int j = 0; j < 4; j++ )
            {
                akOutlineSquare[i].SetColor3( 0, j, 1, 0, 0 ) ;
            }
        }
        // neg x clipping:
        akOutlineSquare[0].SetPosition3( 0, 0, 0, 0 ) ;
        akOutlineSquare[0].SetPosition3( 1, 0, 0, m_fZ ) ;
        akOutlineSquare[0].SetPosition3( 2, 0, m_fY, m_fZ ) ;
        akOutlineSquare[0].SetPosition3( 3, 0, m_fY, 0 ) ;

        // pos x clipping:
        akOutlineSquare[1].SetPosition3( 0, m_fX, 0, m_fZ ) ;
        akOutlineSquare[1].SetPosition3( 1, m_fX, 0, 0 ) ;
        akOutlineSquare[1].SetPosition3( 2, m_fX, m_fY, 0 ) ;
        akOutlineSquare[1].SetPosition3( 3, m_fX, m_fY, m_fZ ) ;

        // neg y clipping:
        akOutlineSquare[2].SetPosition3( 0, m_fX, 0, m_fZ ) ;
        akOutlineSquare[2].SetPosition3( 1, 0, 0, m_fZ ) ;
        akOutlineSquare[2].SetPosition3( 2, 0, 0, 0 ) ;
        akOutlineSquare[2].SetPosition3( 3, m_fX, 0, 0 ) ;
        // pos y clipping:
        akOutlineSquare[3].SetPosition3( 0, m_fX, m_fY, 0 ) ;
        akOutlineSquare[3].SetPosition3( 1, 0, m_fY, 0 ) ;
        akOutlineSquare[3].SetPosition3( 2, 0, m_fY, m_fZ ) ;
        akOutlineSquare[3].SetPosition3( 3, m_fX, m_fY, m_fZ ) ;

        // neg z clipping:
        akOutlineSquare[4].SetPosition3( 0, m_fX, 0, 0 ) ;
        akOutlineSquare[4].SetPosition3( 1, 0, 0, 0 ) ;
        akOutlineSquare[4].SetPosition3( 2, 0, m_fY, 0 ) ;
        akOutlineSquare[4].SetPosition3( 3, m_fX, m_fY, 0 ) ;

        // pos z clipping:
        akOutlineSquare[5].SetPosition3( 0, 0, 0, m_fZ ) ;
        akOutlineSquare[5].SetPosition3( 1, m_fX, 0, m_fZ ) ;
        akOutlineSquare[5].SetPosition3( 2, m_fX, m_fY, m_fZ ) ;
        akOutlineSquare[5].SetPosition3( 3, 0, m_fY, m_fZ ) ;

        for ( int i = 0; i < 6; i++ )
        {
            m_akPolyline[i] = new Polyline( new VertexBuffer(akOutlineSquare[i]), true, true );
            m_akPolyline[i].AttachEffect( m_kVertexColor3Shader );
            m_akPolyline[i].Local.SetTranslate(m_kTranslate);
        }

        VertexBuffer kOutlineSquare = new VertexBuffer( kAttributes, 4);
        // arbitrary clipping:
        for ( int i = 0; i < 4; i++ )
        {
            kOutlineSquare.SetColor3( 0, i, 1, 0, 0 ) ;
        }
        kOutlineSquare.SetPosition3( 0, 0, 0, 0 ) ;
        kOutlineSquare.SetPosition3( 1, 0, 0, m_fZ ) ;
        kOutlineSquare.SetPosition3( 2, 0, m_fY, m_fZ ) ;
        kOutlineSquare.SetPosition3( 3, 0, m_fY, 0 ) ;
        m_kClipArb = new Polyline( new VertexBuffer(kOutlineSquare), true, true );
        m_kClipArb.AttachEffect( m_kVertexColor3Shader );
        m_kClipArb.Local.SetTranslate(m_kTranslate);
        m_kArbRotate.AttachChild( m_kClipArb );

        // eye clipping:
        // set up camera
        m_spkEyeCamera = new Camera();
        m_spkEyeCamera.SetFrustum(-0.55f,0.55f,-0.4125f,0.4125f,1.0f,1000.0f);
        Vector3f kCDir = new Vector3f(0.0f,0.0f,1.0f);
        Vector3f kCUp = new Vector3f(0.0f,-1.0f,0.0f);
        Vector3f kCRight = new Vector3f();
        kCDir.Cross(kCUp, kCRight);
        Vector3f kCLoc = new Vector3f(kCDir);
        kCLoc.scaleEquals(-4.0f);
        m_spkEyeCamera.SetFrame(kCLoc,kCDir,kCUp,kCRight);

        for ( int i = 0; i < 4; i++ )
        {
            kOutlineSquare.SetColor3( 0, i, 1, 0, 0 ) ;
        }
        kOutlineSquare.SetPosition3( 0, -.2f, -.2f, m_fZ ) ;
        kOutlineSquare.SetPosition3( 1, m_fX +.2f, -.2f, m_fZ ) ;
        kOutlineSquare.SetPosition3( 2, m_fX +.2f, m_fY +.2f, m_fZ ) ;
        kOutlineSquare.SetPosition3( 3, -.2f, m_fY +.2f, m_fZ ) ;
        m_kClipEye = new Polyline( new VertexBuffer(kOutlineSquare), true, true );
        m_kClipEye.Local.SetTranslate(m_kTranslate);
        m_kClipEye.AttachEffect( m_kVertexColor3Shader );
        m_kClipEye.UpdateGS();
        m_kClipEye.UpdateRS();

        for ( int i = 0; i < 4; i++ )
        {
            kOutlineSquare.SetColor3( 0, i, 1, 0, 0 ) ;
        }
        kOutlineSquare.SetPosition3( 0, -.2f, -.2f, 1.0f ) ;
        kOutlineSquare.SetPosition3( 1, m_fX +.2f, -.2f, 1.0f ) ;
        kOutlineSquare.SetPosition3( 2, m_fX +.2f, m_fY +.2f, 1.0f ) ;
        kOutlineSquare.SetPosition3( 3, -.2f, m_fY +.2f, 1.0f ) ;
        m_kClipEyeInv = new Polyline( new VertexBuffer(kOutlineSquare), true, true );
        m_kClipEyeInv.Local.SetTranslate(m_kTranslate);
        m_kClipEyeInv.AttachEffect( m_kVertexColor3Shader );
        m_kClipEyeInv.UpdateGS();
        m_kClipEyeInv.UpdateRS();
    }

    private VertexColor3Effect m_kVertexColor3Shader;


    /** axis-aligned clip plane polylines: */
    private Polyline[] m_akPolyline;
    /** arbitrary clip plane polyline: */
    private Polyline m_kClipArb;
    /** eye clip plane polyline: */
    private Polyline m_kClipEye;
    /** inverse-eye clip plane polyline: */
    private Polyline m_kClipEyeInv;
    /** enables/disables displaying clip planes*/
    private boolean[] m_abDisplayPolyline = new boolean[]{false,false,false,false,false,false};

    /** Screen camera for displaying the eye clip planes in screen-coordinates: */
    private Camera m_spkEyeCamera;
    /** Node for rotating the arbitrary clip plane with the mouse trackball: */
    private Node m_kArbRotate = new Node();

    /** Arbitrary clip plane equation: */
    private Vector4f m_kArbitraryClip;
    /** Enables/Disables displaying the arbitrary clip plane: */
    private boolean m_bDisplayClipArb = false;
    /** Enables/Disables displaying the eye clip plane: */
    private boolean m_bDisplayClipEye = false;
    /** Enables/Disables displaying the inverse-eye clip plane: */
    private boolean m_bDisplayClipEyeInv = false;

}
