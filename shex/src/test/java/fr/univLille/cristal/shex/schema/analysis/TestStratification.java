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
package fr.univLille.cristal.shex.schema.analysis;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import fr.univLille.cristal.shex.ConfigurationTest;
import fr.univLille.cristal.shex.exception.NotStratifiedException;
import fr.univLille.cristal.shex.schema.ShexSchema;
import fr.univLille.cristal.shex.schema.parsing.GenParser;
import fr.univLille.cristal.shex.shexTest.TestResultForTestReport;
import fr.univLille.cristal.shex.util.RDFFactory;

/**
 * 
 * @author Iovka Boneva
 * 10 oct. 2017
 */
@RunWith(Parameterized.class)
public class TestStratification {
private static final RDFFactory RDF_FACTORY = RDFFactory.getInstance();
	
	private static final String TEST_DIR = "/home/jdusart/Documents/Shex/workspace/shexTest/";
	private static final String DATA_DIR = TEST_DIR + "validation/";
	private static final String SCHEMAS_DIR = TEST_DIR + "schemas/";

	private static final String GITHUB_URL = "https://raw.githubusercontent.com/shexSpec/shexTest/master/";
	private static final String MANIFEST_FILE = TEST_DIR + "validation/manifest.ttl";
	private static final Resource VALIDATION_FAILURE_CLASS = RDF_FACTORY.createIRI("http://www.w3.org/ns/shacl/test-suite#ValidationFailure");
	private static final Resource VALIDATION_TEST_CLASS = RDF_FACTORY.createIRI("http://www.w3.org/ns/shacl/test-suite#ValidationTest");
	private static final IRI RDF_TYPE = RDF_FACTORY.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	
	private static final Set<TestResultForTestReport> failed = new HashSet<TestResultForTestReport>();
	private static final Set<TestResultForTestReport> passed = new HashSet<TestResultForTestReport>();
	private static final Set<TestResultForTestReport> skiped = new HashSet<TestResultForTestReport>();
	private static final Set<TestResultForTestReport> errors = new HashSet<TestResultForTestReport>();
	
	
	@Parameters
	public static Collection<Object[]> data() throws IOException {
		Path testStratification = Paths.get(ConfigurationTest.shexTestPath.toString(),"success","TestStratification");
		List<Object[]> listOfParameters = ConfigurationTest.getTestFromDirectory(testStratification, 0);
		
		Path errorStratification = Paths.get(ConfigurationTest.shexTestPath.toString(),"failure","TestExtra");
		listOfParameters.addAll(ConfigurationTest.getTestFromDirectory(errorStratification, 1));
		
		errorStratification = Paths.get(ConfigurationTest.shexTestPath.toString(),"failure","TestNot");
		listOfParameters.addAll(ConfigurationTest.getTestFromDirectory(errorStratification, 1));
		
		return listOfParameters;
	}
		
	@Parameter
	public Path schemaFile;
	@Parameter(1)
	public int status;
	
	
	// status == 0 -> success
	// status == 1 -> NotStratifiedException
	@Test
	public void parse (){
		try {
			ShexSchema schema = GenParser.parseSchema(schemaFile);
		}catch(NotStratifiedException e) {
			if (status!=1) {
				fail("Error: schema not stratified for test: "+schemaFile+".");
			}
		}catch(Exception e){
//			System.out.println(schemaFile);
//			System.out.println(e.getClass().getName()+':'+e.getMessage());
//			e.printStackTrace();
			fail(schemaFile+" create error "+e.getClass().getName()+": "+e.getMessage());
		}
	}

}
