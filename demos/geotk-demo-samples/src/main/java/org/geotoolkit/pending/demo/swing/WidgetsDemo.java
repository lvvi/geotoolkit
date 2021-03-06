
package org.geotoolkit.pending.demo.swing;

import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.gui.swing.contexttree.TreePopupItem;
import org.geotoolkit.gui.swing.contexttree.menu.ContextPropertyItem;
import org.geotoolkit.gui.swing.contexttree.menu.DeleteItem;
import org.geotoolkit.gui.swing.contexttree.menu.LayerFeatureItem;
import org.geotoolkit.gui.swing.contexttree.menu.LayerPropertyItem;
import org.geotoolkit.gui.swing.contexttree.menu.NewGroupItem;
import org.geotoolkit.gui.swing.contexttree.menu.SeparatorItem;
import org.geotoolkit.gui.swing.contexttree.menu.ZoomToLayerItem;
import org.geotoolkit.gui.swing.propertyedit.ClearSelectionAction;
import org.geotoolkit.gui.swing.propertyedit.DeleteSelectionAction;
import org.geotoolkit.gui.swing.propertyedit.LayerFilterPropertyPanel;
import org.geotoolkit.gui.swing.propertyedit.LayerGeneralPanel;
import org.geotoolkit.gui.swing.propertyedit.LayerStylePropertyPanel;
import org.geotoolkit.gui.swing.propertyedit.PropertyPane;
import org.geotoolkit.gui.swing.propertyedit.filterproperty.JCQLPropertyPanel;
import org.geotoolkit.gui.swing.propertyedit.styleproperty.JAdvancedStylePanel;
import org.geotoolkit.gui.swing.propertyedit.styleproperty.JClassificationIntervalStylePanel;
import org.geotoolkit.gui.swing.propertyedit.styleproperty.JClassificationSingleStylePanel;
import org.geotoolkit.gui.swing.propertyedit.styleproperty.JSimpleStylePanel;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.pending.demo.rendering.PortrayalDemo;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gui.swing.propertyedit.JLayerCRSPane;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.StyleConstants;
import org.opengis.referencing.operation.TransformException;

public class WidgetsDemo extends javax.swing.JFrame {

    private static final MutableStyleFactory SF = (MutableStyleFactory) FactoryFinder.getStyleFactory(
                                                   new Hints(Hints.STYLE_FACTORY, MutableStyleFactory.class));

    public WidgetsDemo() throws DataStoreException, URISyntaxException {
        Demos.init();

        initComponents();

        //link bars to the map widget ----------------------
        guiCoordBar.setMap(guiMap);
        guiEditBar.setMap(guiMap);
        guiSelectBar.setMap(guiMap);
        guiNavBar.setMap(guiMap);
        guiInfoBar.setMap(guiMap);


        //set the mapcontext in the tree and the map -------------------------
        final MapContext context = MapBuilder.createContext();
        final FeatureCollection features = openShapeFile();
        final MutableStyle featureStyle = SF.style(StyleConstants.DEFAULT_LINE_SYMBOLIZER);
        final FeatureMapLayer featureLayer = MapBuilder.createFeatureLayer(features, featureStyle);
        context.layers().add(featureLayer);

        guiMap.getContainer().setContext(context);
        guiTree.setContext(context);

        try {
            guiMap.getCanvas().setVisibleArea(context.getBounds());
        } catch (NoninvertibleTransformException | TransformException | IOException ex) {
            Logger.getLogger("org.geotoolkit.pending.demo.swing").log(Level.SEVERE, null, ex);
        }


        //configure actions on the tree --------------------------------------
        LayerFeatureItem item = new LayerFeatureItem();
        item.actions().add(new ClearSelectionAction());
        item.actions().add(new DeleteSelectionAction());

        guiTree.controls().add(item);
        guiTree.controls().add(new NewGroupItem());
        guiTree.controls().add(new ZoomToLayerItem());
        guiTree.controls().add(new SeparatorItem());
        guiTree.controls().add(new DeleteItem());
        guiTree.controls().add(new SeparatorItem());

        LayerPropertyItem property = new LayerPropertyItem();
        List<PropertyPane> lstproperty = new ArrayList<PropertyPane>();
        lstproperty.add(new LayerGeneralPanel());
        lstproperty.add(new JLayerCRSPane());

        LayerFilterPropertyPanel filters = new LayerFilterPropertyPanel();
        filters.addPropertyPanel(MessageBundle.format("filter"),new JCQLPropertyPanel());
        lstproperty.add(filters);

        LayerStylePropertyPanel styles = new LayerStylePropertyPanel();
        styles.addPropertyPanel(MessageBundle.format("analyze"),new JSimpleStylePanel());
        styles.addPropertyPanel(MessageBundle.format("analyze"),new JClassificationSingleStylePanel());
        styles.addPropertyPanel(MessageBundle.format("analyze"),new JClassificationIntervalStylePanel());
        styles.addPropertyPanel(MessageBundle.format("sld"),new JAdvancedStylePanel());
        lstproperty.add(styles);

        property.setPropertyPanels(lstproperty);

        guiTree.controls().add(property);
        guiTree.controls().add(new ContextPropertyItem());
        guiTree.revalidate();

        //some actions may need to know the related map
        for(TreePopupItem menuItem : guiTree.controls()){
            menuItem.setMapView(guiMap);
        }




    }

    private static FeatureCollection openShapeFile() throws DataStoreException, URISyntaxException {
        final Map<String,Serializable> params = new HashMap<String,Serializable>();
        params.put("path", PortrayalDemo.class.getResource("/data/world/Countries.shp").toURI());

        final FeatureStore store = FeatureStoreFinder.open(params);
        final Session session = store.createSession(true);
        final Query query = QueryBuilder.all(store.getNames().iterator().next());
        final FeatureCollection collection = session.getFeatureCollection(query);
        return collection;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        guiMap = new org.geotoolkit.gui.swing.render2d.JMap2D();
        guiNavBar = new org.geotoolkit.gui.swing.render2d.control.JNavigationBar();
        guiEditBar = new org.geotoolkit.gui.swing.render2d.control.JEditionBar();
        guiCoordBar = new org.geotoolkit.gui.swing.render2d.control.JCoordinateBar();
        guiInfoBar = new org.geotoolkit.gui.swing.render2d.control.JInformationBar();
        guiTree = new org.geotoolkit.gui.swing.contexttree.JContextTree();
        guiSelectBar = new org.geotoolkit.gui.swing.render2d.control.JSelectionBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        guiNavBar.setRollover(true);

        guiEditBar.setRollover(true);

        guiCoordBar.setRollover(true);

        guiInfoBar.setRollover(true);

        guiSelectBar.setRollover(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(guiCoordBar, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(guiNavBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiInfoBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiSelectBar, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiEditBar, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE))
                    .addComponent(guiMap, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(guiTree, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(guiTree, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(guiNavBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(guiInfoBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(guiSelectBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(guiEditBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(guiMap, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(guiCoordBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger("org.geotoolkit.pending.demo.swing").log(Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new WidgetsDemo().setVisible(true);
                } catch (DataStoreException | URISyntaxException ex) {
                    Logger.getLogger("org.geotoolkit.pending.demo.swing").log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.geotoolkit.gui.swing.render2d.control.JCoordinateBar guiCoordBar;
    private org.geotoolkit.gui.swing.render2d.control.JEditionBar guiEditBar;
    private org.geotoolkit.gui.swing.render2d.control.JInformationBar guiInfoBar;
    private org.geotoolkit.gui.swing.render2d.JMap2D guiMap;
    private org.geotoolkit.gui.swing.render2d.control.JNavigationBar guiNavBar;
    private org.geotoolkit.gui.swing.render2d.control.JSelectionBar guiSelectBar;
    private org.geotoolkit.gui.swing.contexttree.JContextTree guiTree;
    // End of variables declaration//GEN-END:variables

}
