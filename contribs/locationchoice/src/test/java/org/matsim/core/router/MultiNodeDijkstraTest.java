/* *********************************************************************** *
 * project: org.matsim.*
 * MultiNodeDijkstraTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.MultiNodeDijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vehicles.Vehicle;

/**
 * Use the same test cases as for the PT MultiNodeDijkstra.
 * 
 * @author cdobler
 */
public class MultiNodeDijkstraTest {

	private final static Logger log = Logger.getLogger(MultiNodeDijkstraTest.class);

	private MultiNodeDijkstra makeMultiNodeDikstra(Network network, TravelDisutility travelDisutility, TravelTime travelTime,
			boolean fastRouter) {
		if (fastRouter) {
			return (MultiNodeDijkstra) new FastMultiNodeDijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);
		} else return (MultiNodeDijkstra) new MultiNodeDijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);
	}
	
	@Test
	public void testMultipleStarts() {
		testMultipleStarts(true);
		testMultipleStarts(false);
	}
	
	public void testMultipleStarts(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(1)), 1.0, 1.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 3.0, 3.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(3)), 2.0, 2.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 0.0, 0.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);

		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("1", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(new IdImpl(1), 2.0, 5.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(new IdImpl(1), 2.0, 1.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("1", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	public void testMultipleEnds() {
		testMultipleEnds(true);
		testMultipleEnds(false);
	}
	
	public void testMultipleEnds(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 4.0, 4.0);
		tc.setData(new IdImpl(5), 3.0, 3.0);
		tc.setData(new IdImpl(6), 7.0, 7.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 0.0, 0.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 5.0, 5.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 4.0, 4.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(6)), 1.0, 1.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(new IdImpl(4), 3.0, 1.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(new IdImpl(6), 7.0, 3.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("6", p.links.get(2).getId().toString());
	}

	@Test
	public void testMultipleStartsAndEnds() {
		testMultipleStartsAndEnds(true);
		testMultipleStartsAndEnds(false);
	}
	
	public void testMultipleStartsAndEnds(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 4.0, 4.0);
		tc.setData(new IdImpl(5), 3.0, 3.0);
		tc.setData(new IdImpl(6), 7.0, 7.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 4.0, 4.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(3)), 3.0, 3.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 5.0, 5.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 4.0, 4.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(6)), 1.0, 1.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);

		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(new IdImpl(3), 3.0, 1.0);
		tc.setData(new IdImpl(4), 3.0, 1.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("3", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(new IdImpl(3), 3.0, 4.0);
		tc.setData(new IdImpl(6), 7.0, 3.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("6", p.links.get(2).getId().toString());
	}

	@Test
	public void testStartViaFaster() {
		testStartViaFaster(true);
		testStartViaFaster(false);
	}
	
	/**
	 * Both nodes 1 and 4 are part of the start set. Even if the path from 1 to the
	 * target leads over node 4, it may be faster, due to the intial cost values.
	 * Test that the route does not cut at node 4 as the first node backwards from
	 * the start set.
	 */
	public void testStartViaFaster(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(1)), 1.0, 1.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 4.0, 4.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 0.0, 0.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("1", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	public void testEndViaFaster() {
		testEndViaFaster(true);
		testEndViaFaster(false);
	}
	
	public void testEndViaFaster(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(8)), 3.0, 3.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 1.0, 1.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
//		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
//		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
//		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(1.0, 1.0));
//		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
//		toNodes.put(f.network.getNodes().get(new IdImpl(8)), new InitialNode(3.0, 3.0));
//		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(1.0, 1.0));
//
//		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	public void testOnlyFromToSameNode() {
		testOnlyFromToSameNode(true);
		testOnlyFromToSameNode(false);
	}
	
	public void testOnlyFromToSameNode(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(0, p.links.size());
		Assert.assertEquals(1, p.nodes.size());
		Assert.assertEquals("2", p.nodes.get(0).getId().toString());
	}

	@Test
	public void testSameNodeInFromToSetCheapest() {
		testSameNodeInFromToSetCheapest(true);
		testSameNodeInFromToSetCheapest(false);
	}
	
	/**
	 * Tests that a path is found if some links are in the set of start
	 * as well as in the set of end nodes and the path only containing
	 * of this node is the cheapest.
	 */
	public void testSameNodeInFromToSetCheapest(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(6)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(0, p.links.size());
		Assert.assertEquals(1, p.nodes.size());
		Assert.assertEquals("4", p.nodes.get(0).getId().toString());
	}

	@Test
	public void testSameNodeInFromToSetNotCheapest() {
		testSameNodeInFromToSetNotCheapest(true);
		testSameNodeInFromToSetNotCheapest(false);
	}
	
	/**
	 * Tests that a path is found if some links are in the set of start
	 * as well as in the set of end nodes, but the path only containing
	 * of this node is the not the cheapest.
	 */
	public void testSameNodeInFromToSetNotCheapest(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 10.0, 10.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 8.0, 8.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(6)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("6", p.links.get(2).getId().toString());
	}

	@Test
	public void testSomeEndNodesNotReachable() {
		testSomeEndNodesNotReachable(true);
		testSomeEndNodesNotReachable(false);
	}
	
	/**
	 * Tests that a route is found even if not all given end nodes are reachable
	 */
	public void testSomeEndNodesNotReachable(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(1)), 3.0, 3.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(3)), 3.0, 3.0)); // cannot be reached!
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	public void testSomeStartNodesNotUseable() {
		testSomeStartNodesNotUseable(true);
		testSomeStartNodesNotUseable(false);
	}
	
	/**
	 * Tests that a route is found even if not all given start nodes lead to an end node
	 */
	public void testSomeStartNodesNotUseable(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(4)), 3.0, 3.0)); // cannot lead to 5 or 6
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(5)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(3)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull("no path found!", p);
		Assert.assertEquals(3, p.links.size());
		Assert.assertEquals("2", p.links.get(0).getId().toString());
		Assert.assertEquals("7", p.links.get(1).getId().toString());
		Assert.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	public void testImpossibleRoute() {
		testImpossibleRoute(true);
		testImpossibleRoute(false);
	}
	
	public void testImpossibleRoute(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(1)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(2)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNull("wow, impossible path found!", p);
	}

	/*
	 * Ensure that the initial time and cost values are not taken into
	 * account in the path.
	 */
	@Test
	public void testInitialValuesCorrection() {
		testInitialValuesCorrection(true);
		testInitialValuesCorrection(false);
	}
	
	public void testInitialValuesCorrection(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 100.0, 200.0);
		tc.setData(new IdImpl(2), 100.0, 200.0);
		tc.setData(new IdImpl(3), 100.0, 200.0);
		tc.setData(new IdImpl(4), 100.0, 200.0);
		tc.setData(new IdImpl(5), 100.0, 200.0);
		tc.setData(new IdImpl(6), 100.0, 200.0);
		tc.setData(new IdImpl(7), 100.0, 200.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(1)), 10000.0, 10000.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(new IdImpl(6)), 20000.0, 20000.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assert.assertNotNull(p);
		Assert.assertEquals(300.0, p.travelTime, 0.0);
		Assert.assertEquals(600.0, p.travelCost, 0.0);
	}
	
	/*package*/ static Path createPath(Dijkstra dijsktra, Node fromNode, Node toNode) {
		Path path = dijsktra.calcLeastCostPath(fromNode, toNode, 0., null, null);
		
		if (path == null) return path;
		
		for(Node node : path.nodes) log.info("\t\t" + node.getId());
		for(Link link : path.links) log.info("\t\t" + link.getId());
		log.info(path.travelCost);
		log.info(path.travelTime);
		
		return path;
	}
	
	/**
	 * Creates a simple network to be used in tests.
	 *
	 * <pre>
	 *   (1)                       (4)
	 *      \                     /
	 *       \_1               4_/
	 *        \                 /
	 *   (2)-2-(7)-----7-----(8)-5-(5)
	 *        /                 \
	 *       /_3               6_\
	 *      /                     \
	 *   (3)                       (6)
	 * </pre>
	 *
	 * @author mrieser
	 */
	/*package*/ static class Fixture {
		/*package*/ NetworkImpl network;

		public Fixture() {
			this.network = NetworkImpl.createNetwork();
			Node node1 = this.network.createAndAddNode(new IdImpl(1), new CoordImpl(1000,    0));
			Node node2 = this.network.createAndAddNode(new IdImpl(2), new CoordImpl( 500,    0));
			Node node3 = this.network.createAndAddNode(new IdImpl(3), new CoordImpl(   0,    0));
			Node node4 = this.network.createAndAddNode(new IdImpl(4), new CoordImpl(1000, 2000));
			Node node5 = this.network.createAndAddNode(new IdImpl(5), new CoordImpl( 500, 2000));
			Node node6 = this.network.createAndAddNode(new IdImpl(6), new CoordImpl(   0, 2000));
			Node node7 = this.network.createAndAddNode(new IdImpl(7), new CoordImpl( 500,  500));
			Node node8 = this.network.createAndAddNode(new IdImpl(8), new CoordImpl( 500, 1500));
			this.network.createAndAddLink(new IdImpl(1), node1, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(2), node2, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(3), node3, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(4), node8, node4, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(5), node8, node5, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(6), node8, node6, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(7), node7, node8, 1000.0, 10.0, 2000.0, 1);
		}
	}

	/*package*/ static class TestTimeCost implements TravelTime, TravelDisutility {

		private final Map<Id, Double> travelTimes = new HashMap<Id, Double>();
		private final Map<Id, Double> travelCosts = new HashMap<Id, Double>();

		public void setData(final Id id, final double travelTime, final double travelCost) {
			this.travelTimes.put(id, Double.valueOf(travelTime));
			this.travelCosts.put(id, Double.valueOf(travelCost));
		}

		@Override
		public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
			return this.travelTimes.get(link.getId()).doubleValue();
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return this.travelCosts.get(link.getId()).doubleValue();
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 0;
		}

	}
}
