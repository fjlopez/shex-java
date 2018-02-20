/*******************************************************************************
 * Copyright (C) 2018 Université de Lille - Inria
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.univLille.cristal.shex.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import fr.univLille.cristal.shex.graph.NeighborTriple;
import fr.univLille.cristal.shex.graph.RDFGraph;
import fr.univLille.cristal.shex.schema.ShapeExprLabel;
import fr.univLille.cristal.shex.schema.ShexSchema;
import fr.univLille.cristal.shex.schema.abstrsynt.NodeConstraint;
import fr.univLille.cristal.shex.schema.abstrsynt.Shape;
import fr.univLille.cristal.shex.schema.abstrsynt.ShapeAnd;
import fr.univLille.cristal.shex.schema.abstrsynt.ShapeExpr;
import fr.univLille.cristal.shex.schema.abstrsynt.ShapeExprRef;
import fr.univLille.cristal.shex.schema.abstrsynt.ShapeExternal;
import fr.univLille.cristal.shex.schema.abstrsynt.ShapeNot;
import fr.univLille.cristal.shex.schema.abstrsynt.ShapeOr;
import fr.univLille.cristal.shex.schema.abstrsynt.TripleConstraint;
import fr.univLille.cristal.shex.schema.abstrsynt.TripleExpr;
import fr.univLille.cristal.shex.schema.analysis.ShapeExpressionVisitor;

/**
 * 
 * @author Jérémie Dusart
 * 7 feb. 2018
 */

public class RecursiveValidation implements ValidationAlgorithm {
	private RDFGraph graph;
	private SORBEGenerator sorbeGenerator;
	private ShexSchema schema;
	private RecursiveTyping typing;
	
	private DynamicCollectorOfTripleConstraint collectorTC;
	
	
	public RecursiveValidation(ShexSchema schema, RDFGraph graph) {
		super();
		this.graph = graph;
		this.sorbeGenerator = new SORBEGenerator();
		this.schema = schema;
		this.collectorTC = new DynamicCollectorOfTripleConstraint();
		this.typing = new RecursiveTyping();
	}
	
	@Override
	public Typing getTyping() {
		return typing;
	}	
	
	@Override
	public void validate(Value focusNode, ShapeExprLabel label) {
//		if (!this.graph.getAllNodes().contains(focusNode)) {
//			System.err.println("!!/?. "+focusNode+" not in the graph.");
//			throw new IllegalArgumentException(focusNode+" does not belong to the graph.");
//		}
		recursiveValidation(focusNode,label);
		if (typing.contains(focusNode, label)) {
			typing.keepLastSessionOfHypothesis();
		} else {
			typing.removeLastSessionOfHypothesis();
		}
	}
	
	protected void recursiveValidation(Value focusNode, ShapeExprLabel label) {
		this.typing.addHypothesis(focusNode, label);
		EvaluateShapeExpressionVisitor visitor = new EvaluateShapeExpressionVisitor(focusNode);
		schema.getShapeMap().get(label).accept(visitor);
		if (!visitor.result)
			this.typing.removeHypothesis(focusNode, label);
		
	}
	
	class EvaluateShapeExpressionVisitor extends ShapeExpressionVisitor<Boolean> {
		private Value node; 
		private Boolean result;
		
		public EvaluateShapeExpressionVisitor(Value one) {
			this.node = one;
		}

		@Override
		public Boolean getResult() {
			if (result == null) return false;
			return result;
		}
		
		@Override
		public void visitShapeAnd(ShapeAnd expr, Object... arguments) {
			for (ShapeExpr e : expr.getSubExpressions()) {
				e.accept(this);
				if (!result) break;
			}
		}

		@Override
		public void visitShapeOr(ShapeOr expr, Object... arguments) {
			for (ShapeExpr e : expr.getSubExpressions()) {
				e.accept(this);
				if (result) break;
			}
		}
		
		@Override
		public void visitShapeNot(ShapeNot expr, Object... arguments) {
			expr.getSubExpression().accept(this);
			result = !result;
		}
		
		@Override
		public void visitShape(Shape expr, Object... arguments) {
			result = isLocallyValid(node, expr);
		}

		@Override
		public void visitNodeConstraint(NodeConstraint expr, Object... arguments) {
			result = expr.contains(node);
		}

		@Override
		public void visitShapeExprRef(ShapeExprRef ref, Object[] arguments) {
			ref.getShapeDefinition().accept(this);
		}

		@Override
		public void visitShapeExternal(ShapeExternal shapeExt, Object[] arguments) {
			throw new UnsupportedOperationException("Not yet implemented.");
		}
	}
	
	
	private boolean isLocallyValid (Value node, Shape shape) {
		TripleExpr tripleExpression = this.sorbeGenerator.getSORBETripleExpr(shape);

		List<TripleConstraint> constraints = collectorTC.getResult(tripleExpression);
		if (constraints.size() == 0) {
			if (!shape.isClosed()) {
				return true;
			} else {
				if (graph.listOutNeighbours(node).size()==0) {
					return true;
				} else {
					return false;
				}
			}
		}
		
		Set<IRI> inversePredicate = new HashSet<IRI>();
		Set<IRI> forwardPredicate = new HashSet<IRI>();
		for (TripleConstraint tc:constraints) {
			if (tc.getProperty().isForward()) {
				forwardPredicate.add(tc.getProperty().getIri());
			}else {
				inversePredicate.add(tc.getProperty().getIri());
			}
		}
		
		List<NeighborTriple> neighbourhood = graph.listInNeighboursWithPredicate(node, inversePredicate);
		if (shape.isClosed()) {
			neighbourhood.addAll(graph.listOutNeighbours(node));
		} else {
			neighbourhood.addAll(graph.listOutNeighboursWithPredicate(node,forwardPredicate));
		}
		
		// Match using only predicate and recursive test. The following line are the only difference with refine validation
		Matcher matcher1 = new MatcherPredicateOnly();
		Map<NeighborTriple,List<TripleConstraint>> matchingTC1 = Matcher.collectMatchingTC(neighbourhood, constraints, matcher1);

		for(Entry<NeighborTriple,List<TripleConstraint>> entry:matchingTC1.entrySet()) {
			List<TripleConstraint> possibility = entry.getValue();
			if (possibility.isEmpty() & ! shape.getExtraProperties().contains(entry.getKey().getPredicate()))
				return false;
			for (TripleConstraint tc:possibility) {
				Value destNode = entry.getKey().getOpposite();
				if (! this.typing.contains(destNode, tc.getShapeExpr().getId())) {
					this.recursiveValidation(destNode, tc.getShapeExpr().getId());
				}
			}
		}


		Matcher matcher2 = new MatcherPredicateAndValue(this.getTyping()); 
		Map<NeighborTriple,List<TripleConstraint>> matchingTC2 = Matcher.collectMatchingTC(neighbourhood, constraints, matcher2);

		// Check that the neighbor that cannot be match to a constraint are in extra
		Iterator<Map.Entry<NeighborTriple,List<TripleConstraint>>> iteMatchingTC = matchingTC2.entrySet().iterator();
		while(iteMatchingTC.hasNext()) {
			Entry<NeighborTriple, List<TripleConstraint>> listTC = iteMatchingTC.next();
			if (listTC.getValue().isEmpty()) {
				if (! shape.getExtraProperties().contains(listTC.getKey().getPredicate())){
					return false;
				}
				iteMatchingTC.remove();
			}
		}
		
		// Create a BagIterator for all possible bags induced by the matching triple constraints
		List<List<TripleConstraint>> listMatchingTC = new ArrayList<List<TripleConstraint>>(matchingTC2.values());
		BagIterator bagIt = new BagIterator(listMatchingTC);

		IntervalComputation intervalComputation = new IntervalComputation(this.collectorTC);
		
		while(bagIt.hasNext()){
			Bag bag = bagIt.next();
			tripleExpression.accept(intervalComputation, bag, this);
			if (intervalComputation.getResult().contains(1)) {
				return true;
			}
		}

		return false;
	}	


}
