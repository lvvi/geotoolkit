/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Johann Sorel
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.gui.swing.render2d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import org.geotoolkit.display.canvas.control.NeverFailMonitor;
import org.geotoolkit.display2d.canvas.J2DCanvas;
import org.geotoolkit.display2d.canvas.J2DCanvasSwing;
import org.geotoolkit.display2d.canvas.SwingVolatileGeoComponent;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.gui.swing.util.BufferLayout;
import org.geotoolkit.gui.swing.render2d.decoration.ColorDecoration;
import org.geotoolkit.gui.swing.render2d.decoration.DefaultInformationDecoration;
import org.geotoolkit.gui.swing.render2d.decoration.InformationDecoration;
import org.geotoolkit.gui.swing.render2d.decoration.MapDecoration;
import org.apache.sis.referencing.CommonCRS;
import static org.apache.sis.util.ArgumentChecks.*;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.display.canvas.AbstractCanvas;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class JMap2D extends JPanel{

    /**
     * The name of the {@linkplain PropertyChangeEvent property change event} fired when the
     * {@linkplain AWTCanvas2D#getHandler canvas handler} changed.
     */
    public static final String HANDLER_PROPERTY = "handler";
    private static final MapDecoration[] EMPTY_OVERLAYER_ARRAY = {};
    private static final Logger LOGGER = Logging.getLogger("org.geotoolkit.gui.swing.render2d");

    private CanvasHandler handler;
    private final J2DCanvas canvas;
    private final JComponent geoComponent;
    private boolean statefull = false;

    private final List<MapDecoration> userDecorations = new ArrayList<>();
    private final JLayeredPane mapDecorationPane = new JLayeredPane();
    private final JLayeredPane userDecorationPane = new JLayeredPane();
    private final JLayeredPane mainDecorationPane = new JLayeredPane();
    private int nextMapDecorationIndex = 1;
    private InformationDecoration informationDecoration = new DefaultInformationDecoration();
    private MapDecoration backDecoration = new ColorDecoration();


    public JMap2D(){
        this(false);
    }

    public JMap2D(final boolean statefull){
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(150,150));
        mapDecorationPane.setLayout(new BufferLayout());
        userDecorationPane.setLayout(new BufferLayout());
        mainDecorationPane.setLayout(new BufferLayout());
        mainDecorationPane.add(informationDecoration.getComponent(), Integer.valueOf(3));
        mainDecorationPane.add(userDecorationPane, Integer.valueOf(2));
        mainDecorationPane.add(mapDecorationPane, Integer.valueOf(1));

        informationDecoration.setMap2D(this);

        add(BorderLayout.CENTER, mainDecorationPane);

        setBackground(Color.WHITE);
        setOpaque(true);

        if(statefull){
            canvas = new J2DCanvasSwing(CommonCRS.WGS84.normalizedGeographic());
            geoComponent = ((J2DCanvasSwing)canvas).getComponent();
        }else{
            geoComponent = new SwingVolatileGeoComponent(CommonCRS.WGS84.normalizedGeographic());
            canvas = ((SwingVolatileGeoComponent)geoComponent).getCanvas();
        }
        canvas.setMonitor(new NeverFailMonitor());


        mapDecorationPane.add(geoComponent, Integer.valueOf(0));
        mapDecorationPane.revalidate();

        canvas.setContainer(new ContextContainer2D(canvas, statefull));
        canvas.setAutoRepaint(true);


        canvas.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if(canvas.isAutoRepaint()){
                    //dont show the painting icon if the cans is in auto render mode
                    // since it may repaint dynamic graphic it would show up all the time
                    return;
                }

                if(AbstractCanvas.RENDERSTATE_KEY.equals(evt.getPropertyName())){
                    final Object state = evt.getNewValue();
                    if(AbstractCanvas.ON_HOLD.equals(state)){
                        getInformationDecoration().setPaintingIconVisible(false);
                    }else if(AbstractCanvas.RENDERING.equals(state)){
                        getInformationDecoration().setPaintingIconVisible(true);
                    }else{
                        getInformationDecoration().setPaintingIconVisible(false);
                    }
                }
            }
        });

        try {
            canvas.setObjectiveCRS(CommonCRS.WGS84.normalizedGeographic());
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }

    }

    /**
     * @return the effective Go2 Canvas.
     */
    public J2DCanvas getCanvas() {
        return canvas;
    }

    public ContextContainer2D getContainer(){
        return (ContextContainer2D) canvas.getContainer();
    }

    /**
     * Must be called when the map2d is not used anymore.
     * to avoid memory leak if it uses thread or other resources
     */
    public void dispose() {
        canvas.dispose();
    }

    public CanvasHandler getHandler(){
        return handler;
    }

    public void setHandler(final CanvasHandler handler){

        if(this.handler != handler) {
            //TODO : check for possible vetos

            final CanvasHandler old = this.handler;

            if (this.handler != null){
                this.handler.uninstall(geoComponent);
            }

            this.handler = handler;

            if (this.handler != null) {
                this.handler.install(geoComponent);
            }

            firePropertyChange(HANDLER_PROPERTY, old, handler);
        }

    }

    //----------------------Use as extend for subclasses------------------------
    protected void setRendering(final boolean render) {
        informationDecoration.setPaintingIconVisible(render);
    }

    //----------------------Over/Sub/information layers-------------------------
    /**
     * set the top InformationDecoration of the map2d widget
     * @param info , can't be null
     */
    public void setInformationDecoration(final InformationDecoration info) {
        ensureNonNull("info decoration", info);

        mainDecorationPane.remove(informationDecoration.getComponent());
        informationDecoration = info;
        mainDecorationPane.add(informationDecoration.getComponent(), Integer.valueOf(3));

        mainDecorationPane.revalidate();
        mainDecorationPane.repaint();
    }

    /**
     * get the top InformationDecoration of the map2d widget
     * @return InformationDecoration
     */
    public InformationDecoration getInformationDecoration() {
        return informationDecoration;
    }

    /**
     * set the decoration behind the map
     * @param back : MapDecoration, can't be null
     */
    public void setBackgroundDecoration(final MapDecoration back) {
        ensureNonNull("background decoration", back);

        mainDecorationPane.remove(backDecoration.getComponent());
        backDecoration = back;
        mainDecorationPane.add(backDecoration.getComponent(), Integer.valueOf(0));

        mainDecorationPane.revalidate();
        mainDecorationPane.repaint();
    }

    /**
     * get the decoration behind the map
     * @return MapDecoration : or null if no back decoration
     */
    public MapDecoration getBackgroundDecoration() {
        return backDecoration;
    }

    /**
     * add a Decoration between the map and the information top decoration
     * @param deco : MapDecoration to add
     */
    public void addDecoration(final MapDecoration deco) {

        if (deco != null && !userDecorations.contains(deco)) {
            deco.setMap2D(this);
            userDecorations.add(deco);
            userDecorationPane.add(deco.getComponent(), Integer.valueOf(userDecorations.indexOf(deco)));
            userDecorationPane.revalidate();
            userDecorationPane.repaint();
        }
    }

    /**
     * insert a MapDecoration at a specific index
     * @param index : index where to isert the decoration
     * @param deco : MapDecoration to add
     */
    public void addDecoration(final int index, final MapDecoration deco) {

        if (deco != null && !userDecorations.contains(deco)) {
            deco.setMap2D(this);
            userDecorations.add(index, deco);
            userDecorationPane.add(deco.getComponent(), Integer.valueOf(userDecorations.indexOf(deco)));
            userDecorationPane.revalidate();
            userDecorationPane.repaint();
        }
    }

    /**
     * get the index of a MapDecoration
     * @param deco : MapDecoration to find
     * @return index of the MapDecoration
     * @throw ClassCastException or NullPointerException
     */
    public int getDecorationIndex(final MapDecoration deco) {
        return userDecorations.indexOf(deco);
    }

    /**
     * remove a MapDecoration
     * @param deco : MapDecoration to remove
     */
    public void removeDecoration(final MapDecoration deco) {
        if (deco != null && userDecorations.contains(deco)) {
            deco.setMap2D(null);
            deco.dispose();
            userDecorations.remove(deco);
            userDecorationPane.remove(deco.getComponent());
            userDecorationPane.revalidate();
            userDecorationPane.repaint();
        }
    }

    /**
     * get an array of all MapDecoration
     * @return array of MapDecoration
     */
    public MapDecoration[] getDecorations() {
        return userDecorations.toArray(EMPTY_OVERLAYER_ARRAY);
    }

    /**
     * add a MapDecoration between the map and the user MapDecoration
     * those MapDecoration can not be removed because they are important
     * for edition/selection/navigation.
     * @param deco : MapDecoration to add
     */
    protected void addMapDecoration(final MapDecoration deco) {
        mapDecorationPane.add(deco.getComponent(), Integer.valueOf(nextMapDecorationIndex));
        nextMapDecorationIndex++;
    }

    //-----------------------------MAP2D----------------------------------------

    /**
     * get the visual component
     * @return Component
     */
    public Component getComponent() {
        return geoComponent;
    }

    /**
     * Can be used to add more components on the side of the map
     * if needed. in any case, dont remove the central component.
     * @return JPanel the container in borderlayout mode
     */
    public JPanel getUIContainer() {
        return this;
    }

}
