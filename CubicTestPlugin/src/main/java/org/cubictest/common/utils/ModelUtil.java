/*******************************************************************************
 * Copyright (c) 2005, 2010 Stein K. Skytteren and Christian Schwarz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Stein K. Skytteren and Christian Schwarz - initial API and implementation
 *******************************************************************************/
package org.cubictest.common.utils;

import java.util.ArrayList;
import java.util.List;

import org.cubictest.common.exception.CubicException;
import org.cubictest.model.AbstractPage;
import org.cubictest.model.Common;
import org.cubictest.model.CommonTransition;
import org.cubictest.model.ExtensionPoint;
import org.cubictest.model.ExtensionStartPoint;
import org.cubictest.model.IActionElement;
import org.cubictest.model.IStartPoint;
import org.cubictest.model.Page;
import org.cubictest.model.PageElement;
import org.cubictest.model.Transition;
import org.cubictest.model.TransitionNode;
import org.cubictest.model.UrlStartPoint;
import org.cubictest.ui.utils.UserInteractionDialogUtil;


/**
 * Util class for model operations.
 * 
 * @author Christian Schwarz
 */
public class ModelUtil {

	public static int TRANSITION_EDIT_NO_CHANGES = 0;
	public static int TRANSITION_EDIT_VALID = 2;
	public static int TRANSITION_EDIT_NOT_VALID = 4;

	
	public static int isLegalTransition(TransitionNode sourceNode, TransitionNode targetNode, 
			boolean isReconnectingSourceNode, boolean isReconnectingTargetNode) {
		
		boolean isNewTransition = !isReconnectingSourceNode && !isReconnectingTargetNode;
		
		if (sourceNode == null)
			return TRANSITION_EDIT_NOT_VALID;
		
		if (sourceNode.equals(targetNode))
			return TRANSITION_EDIT_NOT_VALID;

		if (targetNode instanceof UrlStartPoint)
			return TRANSITION_EDIT_NOT_VALID;
		
		if (targetNode instanceof ExtensionStartPoint)
			return TRANSITION_EDIT_NOT_VALID;

		if (targetNode instanceof Common)
			return TRANSITION_EDIT_NOT_VALID;
		
		if (targetNode == null)
			return TRANSITION_EDIT_NOT_VALID;

		if (sourceNode instanceof Common && !(targetNode instanceof Page))
			return TRANSITION_EDIT_NOT_VALID;

		if (sourceNode instanceof IStartPoint && (isReconnectingSourceNode || isNewTransition)) {
			if (sourceNode.getOutTransitions().size() >= 1) { 
				UserInfo.showInfoDialog("Only one out-transition is allowed from startpoints.\n" +
				"Hint: Create a new test or a tree after the first page/state.");
				return TRANSITION_EDIT_NOT_VALID;
			}
		}
		
		if (isReconnectingSourceNode) {
			if (sourceNode.hasInTransition() && sourceNode.getInTransition().getStart().equals(targetNode)) {
				//cycle between source and target
				UserInfo.showInfoDialog("Cycles are not allowed.\n" +
						"Hint: Create a new page/state instead.");
				return TRANSITION_EDIT_NOT_VALID;
			}
		}
		else {
			//reconnecting target
			
			if (sourceNode.hasInTransition() && sourceNode.getInTransition().getStart().equals(targetNode))  {
				//cycle between source and target
				UserInfo.showInfoDialog("Cycles are not allowed.\n" +
						"Hint: Create a new page/state instead.");
				return TRANSITION_EDIT_NOT_VALID;
			}

			if (targetNode.hasInTransition() && !(sourceNode instanceof Common)) {
				boolean isNoChanges = targetNode.getInTransition().getStart().equals(sourceNode);
				if (isNoChanges) {
					//ok, user has just reconnected to the original state
					return TRANSITION_EDIT_NO_CHANGES;
				}
				else {
					//multiple in-transitions not allowed, unless from Common
					UserInfo.showInfoDialog("Multiple in-connections to a Page/State is not allowed (unless from a Common).");
					return TRANSITION_EDIT_NOT_VALID;
				}
			}
			
			Transition targetInTransition = targetNode.getInTransition();
			if (targetNode.hasInTransition() && targetInTransition.getStart().equals(sourceNode))
				//duplicate transition
				return TRANSITION_EDIT_NOT_VALID;


		}
		
		return TRANSITION_EDIT_VALID;
	}

	public static TransitionNode getFirstNode(List<TransitionNode> nodes) {
		if (nodes == null || nodes.size() == 0) {
			throw new IllegalArgumentException("Node list is null or empty");
		}
		TransitionNode first = nodes.get(0);
		for (TransitionNode nodeToCheck : nodes) {
			for (TransitionNode other : nodes) {
				if (nodeToCheck == other) {
					continue;
				}
				if (ModelUtil.isOnPathToNode(nodeToCheck, other) && ModelUtil.isOnPathToNode(nodeToCheck, first)) {
					first = nodeToCheck;
				}
			}
		}
		return first;
	}
	
	public static boolean hasOnlyOnePathFromNodeToEndOfTest(TransitionNode node) {
		if (node == null) {
			return true;
		}
		int numNext = node.getOutTransitionsWithoutExtensionPoints().size();
		
		if (numNext == 0) {
			return true;
		}
		else if(numNext == 1) {
			return hasOnlyOnePathFromNodeToEndOfTest(node.getOutTransitions().get(0).getEnd());
		}
		else {
			return false;
		}
	}
	
	public static boolean isTestFile(String fileName) {
		if (fileName.endsWith(".aat") || fileName.endsWith(".ats")) {
			return true;
		}
		return false;
	}
	
	public static boolean nodesContainsSingleContinuousPath(List<TransitionNode> nodes) {
		if (nodes.size() <= 1) {
			return true;
		}
		nodes = new ArrayList<TransitionNode>(nodes); //clone
		
		TransitionNode node = ModelUtil.getFirstNode(nodes);
 		if (!ModelUtil.hasOnlyOnePathFromNodeToEndOfTest(node)) {
			return false;
		}
		//we now know there is not a tree after node

		nodes.remove(node);
		while (node.getFirstSuccessor() != null) {
			TransitionNode nextNode = node.getFirstSuccessor();
			if (nodes.contains(nextNode)) {
				nodes.remove(nextNode);
			}
			else {
				return nodes.isEmpty();
			}
			node = nextNode;
		}
		return true;
	}
	
	
	public static TransitionNode getLastNodeInList(List<TransitionNode> list) {
		if (list.size() == 1) {
			return list.get(0);
		}
		list = new ArrayList<TransitionNode>(list); //clone
		
		//initialize node to first node in path:
		TransitionNode node = ModelUtil.getFirstNode(list);

		if (!hasOnlyOnePathFromNodeToEndOfTest(node)) {
			throw new CubicException("Node list has multiple paths (is a tree).");
		}
		if (!nodesContainsSingleContinuousPath(list)) {
			throw new CubicException("Node list was not connected together in a contiuous path.");
		}
		//walk down the path, removing nodes from node list as we go. When the list is empty, we are done
		while (node != null) {
			TransitionNode nextNode = node.getFirstSuccessor();
			if (list.contains(nextNode)) {
				node = nextNode;
			}
			else {
				return node;
			}
		}
		throw new CubicException("List had unexpected nodes in it");
	}

	
	/**
	 * Traverses the test model and finds the start point of the node in the test it belongs to.
	 * @param node
	 * @return
	 */
	public static IStartPoint getStartPoint(TransitionNode node) {
		if (node == null || node.getInTransition() == null) {
			return null;
		}
		else if(node.getInTransition().getStart() instanceof IStartPoint) {
			return (IStartPoint) node.getInTransition().getStart();
		}
		else {
			return getStartPoint(node.getInTransition().getStart());
		}
	}

	/**
	 * Checks if there is a way to get from the node to check to the target node
	 * @param nodeToCheck the node to check all routes from
	 * @param targetNode the node we want to be able to reach
	 */
	public static boolean isOnPathToNode(TransitionNode nodeToCheck, TransitionNode targetNode) {

		String targetNodeId = targetNode.getId();
		
		if (targetNode instanceof ExtensionStartPoint) {
			targetNodeId = ((ExtensionStartPoint) targetNode).getSourceExtensionPointPageId();
		} 
		else if (targetNode instanceof ExtensionPoint) {
			targetNodeId = ((ExtensionPoint) targetNode).getPageId();
		}
		else if (targetNode instanceof Page) {
			targetNodeId = ((Page) targetNode).getId();
		}

		if (nodeToCheck.getId().equals(targetNodeId)) {
			return true;
		}
		
		//recursively check all end nodes and check if we can get to the target node:
		for (Transition outTransition : nodeToCheck.getOutTransitions()) {
			TransitionNode endNode = (TransitionNode) outTransition.getEnd();
			if (isOnPathToNode(endNode, targetNode)) {
				return true;
			}
		}
		return false;
	}

	
	/**
	 * Gets action elements from Abstract page, including those from the connected commons.
	 */
	public static List<IActionElement> getActionElements(AbstractPage page) {
		List<IActionElement> allActionElements = new ArrayList<IActionElement>();
		List<PageElement> elementsTree = new ArrayList<PageElement>();
		if(page instanceof Page) { // process commonTrasitions for pages
			elementsTree.addAll(page.getRootElements());
			List<CommonTransition> commonTransitions = ((Page)page).getCommonTransitions();
			for (CommonTransition at : commonTransitions)
				elementsTree.addAll(((Common)(at).getStart()).getRootElements());			
		}
		
		//get children from root elements
		List<PageElement> flatList = UserInteractionDialogUtil.getFlattenedPageElements(elementsTree);
		for (PageElement pe : flatList) {
			if (!((PageElement) pe).isNot()){
				allActionElements.add(pe);
			}
		}
		return allActionElements;
	}
	
	public static TransitionNode getLastNodeInGraph(TransitionNode node) {
		if (node.getOutTransitionsWithoutExtensionPoints().size() == 0) {
			return node;
		}
		else {
			return getLastNodeInGraph(node.getOutTransitionsWithoutExtensionPoints().get(0).getEnd());
		}
	}
}
