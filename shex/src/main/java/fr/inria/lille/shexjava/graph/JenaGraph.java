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
package fr.inria.lille.shexjava.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/** Wraps an RDF4J graph as {@link RDFGraph}.
 * 
 * @author Jérémie Dusart
 */
public class JenaGraph extends AbstractRDFGraph {
	private final static ValueFactory rdfFactory = SimpleValueFactory.getInstance();
	protected Model jenaModel;
	
	
	public JenaGraph(Model jenaModel) {
		this.jenaModel = jenaModel;
	}


	@Override
	public Iterator<Value> listAllObjectNodes() {
		return new Iterator<Value>() {
			NodeIterator it; { it = jenaModel.listObjects();	}
			
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Value next() {
				return convertJenaRDFNodeToValue(it.next());
			}
		};
	}


	@Override
	public Iterator<Value> listAllSubjectNodes() {
		return new Iterator<Value>() {
			ResIterator it; { it = jenaModel.listSubjects();	}
			
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public Value next() {
				return convertJenaRDFNodeToValue(it.next());
			}
		};
	}


	@Override
	protected Iterator<NeighborTriple> itOutNeighbours(Value focusNode, IRI predicate) {
		return new Iterator<NeighborTriple>() {
			StmtIterator it; { if (convertRDF4JValueToJenaResource(focusNode)==null) {
						it = null;
					}else {
						it = jenaModel.listStatements(convertRDF4JValueToJenaResource(focusNode),
													  convertRDF4JIRIToJenaProperty(predicate),
													  (org.apache.jena.rdf.model.RDFNode) null);
						}
					}
	
			@Override
			public boolean hasNext() {
				if (it == null)
					return false;
				return it.hasNext();
			}

			@Override
			public NeighborTriple next() {
				if (it == null)
					throw new NoSuchElementException();
				Statement next = it.next();
				IRI predicate = convertJenaPropertyToRDF4JIRI(next.getPredicate());
				TCProperty prop = TCProperty.createFwProperty(predicate);
				return new NeighborTriple(focusNode,prop,convertJenaRDFNodeToValue(next.getObject()));
			}
		};
	}


	@Override
	protected Iterator<NeighborTriple> itInNeighbours(Value focusNode, IRI predicate) {
		return new Iterator<NeighborTriple>() {
			StmtIterator it; { it = jenaModel.listStatements(null,
															 convertRDF4JIRIToJenaProperty(predicate),
															 convertRDF4JValueToJenaRDFNode(focusNode));
			}
			
			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public NeighborTriple next() {
				Statement next = it.next();
				IRI predicate = convertJenaPropertyToRDF4JIRI(next.getPredicate());
				TCProperty prop = TCProperty.createInvProperty(predicate);
				return new NeighborTriple(convertJenaRDFNodeToValue(next.getSubject()),prop,focusNode);
			}
		};
	}

	@Override
	public String toString() {
		return jenaModel.toString();
	}
	
	//----------------------------------------------
	// Conversion RDF4J to Jena
	//----------------------------------------------
	
	public org.apache.jena.rdf.model.Property convertRDF4JIRIToJenaProperty(IRI predicate) {
		if (predicate == null)
			return null;
		return jenaModel.createProperty(predicate.stringValue());
	}
	
	public org.apache.jena.rdf.model.Resource convertRDF4JValueToJenaResource(Value value) {
		if (value == null)
			return null;
		if (value instanceof IRI)
			return jenaModel.createResource(value.stringValue());
		if (value instanceof BNode) {
			return jenaModel.createResource(new AnonId(value.stringValue()));
		}
		if (value instanceof Literal)
			return null;
		System.out.println("Need to convert from RDF4J: "+value.getClass()+" > "+value);
		
		return null;
	}
	
	public org.apache.jena.rdf.model.RDFNode convertRDF4JValueToJenaRDFNode(Value value) {
		if (value == null)
			return null;
		if (value instanceof Literal) {
			Literal lvalue = (Literal) value;
			if (lvalue.getLanguage().isPresent())
				return jenaModel.createLiteral(lvalue.stringValue(), lvalue.getLanguage().get());
			return jenaModel.createTypedLiteral(lvalue.stringValue(), lvalue.getDatatype().stringValue());
		} else {
			return convertRDF4JValueToJenaResource(value);
		}
	}
	
	//----------------------------------------------
	// Conversion Jena to RDF4J
	//----------------------------------------------
	
	public IRI convertJenaPropertyToRDF4JIRI(org.apache.jena.rdf.model.Property prop) {
		if (prop == null)
			return null;
		return rdfFactory.createIRI(prop.getURI());
	}
	
	public Value convertJenaRDFNodeToValue(org.apache.jena.rdf.model.RDFNode jenaRes) {
		if (jenaRes == null)
			return null;
		if (jenaRes.isResource()) {
			return convertJenaRDFNodeToResource((org.apache.jena.rdf.model.Resource) jenaRes);
		}else {
			return convertJenaRDFNodeToLiteral((org.apache.jena.rdf.model.Literal) jenaRes);
		}
	}
	
	public Resource convertJenaRDFNodeToResource(org.apache.jena.rdf.model.Resource jenaRes) {
		if (jenaRes == null)
			return null;
		if (jenaRes.isAnon()) {
			return rdfFactory.createBNode(jenaRes.toString());
		}
		return rdfFactory.createIRI(jenaRes.getURI());
	}
	
	public static Literal convertJenaRDFNodeToLiteral(org.apache.jena.rdf.model.Literal jenaLit) {
		if (jenaLit == null)
			return null;
		String value = jenaLit.getLexicalForm();
		String lang = jenaLit.getLanguage();
		if (!lang.equals(""))
			return rdfFactory.createLiteral(value, lang);
		return  rdfFactory.createLiteral(value, rdfFactory.createIRI(jenaLit.getDatatypeURI()));			
	}
	

}
