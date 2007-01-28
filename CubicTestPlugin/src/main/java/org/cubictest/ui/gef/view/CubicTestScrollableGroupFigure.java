/*
 * This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE
 * Version 2, which can be found at http://www.gnu.org/copyleft/gpl.html
 */
package org.cubictest.ui.gef.view;

import org.cubictest.ui.utils.ViewUtil;
import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ScrollPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.graphics.Color;

/**
 * Group figure (e.g. for Page) that uses scrollpane for content.
 * 
 * @author chr_schwarz
 */
public class CubicTestScrollableGroupFigure extends CubicTestGroupFigure {
	public static Color pageColor = new Color(null, 255, 255, 255);
	private CubicTestFreeformLayer pane;
	private ScrollPane scrollpane;
	

	public CubicTestScrollableGroupFigure(String title, boolean fullHeaderWidth) {
		super(title, fullHeaderWidth);
		setLayoutManager(new StackLayout());
	}


	@Override
	public void add(IFigure figure, Object constraint, int index) {
		if (pane == null) {
			initializePane();
		}
		if(figure.equals(header)) {
			pane.add(figure, constraint, index);
		} else {
			super.add(figure,constraint, index);
		}
	}

	public IFigure getContentsPane(){
		return pane;
	}

	private void initializePane() {
		scrollpane = new ScrollPane();
		pane = new CubicTestFreeformLayer(header);
		pane.setLayoutManager(ViewUtil.getFlowLayout());
		scrollpane.setViewport(new FreeformViewport());
		scrollpane.setContents(pane);
		scrollpane.setHorizontalScrollBarVisibility(ScrollPane.NEVER);

		// initializing vertical space, to prevent multiple columns:
		pane.setSize(new Dimension(1, 9999));  
		add(scrollpane);
	}
	
	public ScrollPane getScrollPane() {
		return scrollpane;
	}
}