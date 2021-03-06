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
package fr.inria.lille.shexjava.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;

import fr.inria.lille.shexjava.graph.NeighborTriple;
import fr.inria.lille.shexjava.schema.abstrsynt.TripleConstraint;

/** Defines a custom condition on whether a neighbor triple matches a triple constraint.
 * 
 * @author Iovka Boneva
 * @author Antonin Durey
 *
 */
public interface Matcher extends BiFunction<NeighborTriple, TripleConstraint, Boolean> {

	/** Constructs a list that for all neighbor triple contains the list of triple constraints that the triple matches according to the matcher given as parameter.
	 * 
	 * @param neighbourhood
	 * @param constraints
	 * @param matcher
	 */
	public static LinkedHashMap<NeighborTriple,List<TripleConstraint>> collectMatchingTC (List<NeighborTriple> neighbourhood, List<TripleConstraint> constraints, Matcher matcher) {
		
		LinkedHashMap<NeighborTriple,List<TripleConstraint>> result = new LinkedHashMap<>(neighbourhood.size()); 
		
		for (NeighborTriple triple: neighbourhood) {
			ArrayList<TripleConstraint> matching = new ArrayList<>();
			for (TripleConstraint tc: constraints) {
				if (matcher.apply(triple, tc)) {
					matching.add(tc);
				}
			}	
			result.put(triple,matching);
		}
		return result;
	}
	
}
