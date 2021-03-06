package it.polito.elite.semantic.example;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

/**
 * Example of use of OWL-API 4.x and the HermiT reasoner.
 * Realized with Gradle and Java 8.
 * 
 * Semantic Web course, Politecnico di Torino, Italy
 * 
 * @author Luigi De Russis, Politecnico di Torino,
 *         <a href="https://elite.polito.it">e-Lite</a> group
 * @version 1.0 (2017-02-22)
 *
 */
public class OWLApiExample
{
	
	public static void main(String[] args)
	{
		// init
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		File university = new File("resources/university.owl");
		OWLOntology localUni;
		
		try
		{
			// load the (local) OWL ontology
			localUni = manager.loadOntologyFromOntologyDocument(university);
			System.out.println("Loaded ontology: " + localUni.getOntologyID());
			IRI location = manager.getOntologyDocumentIRI(localUni);
			System.out.println("\tfrom: " + location);
			
			long time = System.currentTimeMillis();
			
			// get and configure a reasoner (HermiT)
			OWLReasonerFactory reasonerFactory = new ReasonerFactory();
			ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
			OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
			
			// create the reasoner instance, classify and compute inferences
			OWLReasoner reasoner = reasonerFactory.createReasoner(localUni, config);
			// perform all the inferences now, to avoid subsequent ad-hoc
			// reasoner calls
			reasoner.precomputeInferences(InferenceType.values());
			
			// init prefix manager
			DefaultPrefixManager pm = new DefaultPrefixManager(null, null,
					"http://elite.polito.it/ontologies/university.owl#");
			// pm.setPrefix("another:", "http://elite.polito.it/ontologies/anotheront.owl#");
			
			// get all the universities
			OWLDataFactory fac = manager.getOWLDataFactory();
			OWLClass universities = fac.getOWLClass(IRI.create(pm.getDefaultPrefix(), "University"));
			NodeSet<OWLNamedIndividual> individualsNodeSet = reasoner.getInstances(universities, false);
			Set<OWLNamedIndividual> individuals = individualsNodeSet.getFlattened();
			
			for (OWLNamedIndividual uni : individuals)
			{
				// print the individual name
				System.out.println("Individual Name: " + pm.getShortForm(uni));
				
				// get university name
				OWLDataProperty universityName = new OWLDataPropertyImpl(
						IRI.create(pm.getDefaultPrefix() + "universityName"));
				System.out.println(EntitySearcher.getDataPropertyValues(uni, universityName, localUni));
				
				// get offered degrees (i.e., "offersDegree" obj property)
				OWLObjectPropertyImpl op = new OWLObjectPropertyImpl(
						IRI.create(pm.getDefaultPrefix() + "offersDegree"));
				Set<OWLIndividual> offersDegree = new HashSet<>(
						EntitySearcher.getObjectPropertyValues(uni, op, localUni));
				for (OWLIndividual degree : offersDegree)
				{
					System.out.println("\n\toffersDegree: " + pm.getShortForm((OWLEntity) degree));
					
					// get degree name
					OWLDataProperty degreeName = new OWLDataPropertyImpl(
							IRI.create(pm.getDefaultPrefix() + "degreeName"));
					System.out.println("\t" + EntitySearcher.getDataPropertyValues(degree, degreeName, localUni));
					
					// get offered courses ("offersCourse" obj property)
					OWLObjectPropertyImpl c = new OWLObjectPropertyImpl(
							IRI.create(pm.getDefaultPrefix() + "offersCourse"));
					Set<OWLIndividual> offersCourse = new HashSet<>(
							EntitySearcher.getObjectPropertyValues(degree, c, localUni));
					for (OWLIndividual course : offersCourse)
					{
						System.out.println("\t\toffersCourse: " + pm.getShortForm((OWLEntity) course));
						
						// get enrolled students ("isFollowed" obj property)
						OWLObjectPropertyImpl f = new OWLObjectPropertyImpl(
								IRI.create(pm.getDefaultPrefix() + "isFollowed"));
						// the following line does not work since we defined
						// only the "follows" property, and this as the inverse
						// Set<OWLIndividual> isFollowed = new HashSet<>(
						//		EntitySearcher.getObjectPropertyValues(course, f, localUni));
						Set<OWLNamedIndividual> isFollowed = reasoner
								.getObjectPropertyValues(course.asOWLNamedIndividual(), f).getFlattened();
						for (OWLIndividual student : isFollowed)
						{
							System.out.println("\t\t\tisFollowed: " + pm.getShortForm((OWLEntity) student));
						}
						
					}
				}
				
				System.out.println("---");
			}
			System.err.println("All universities (" + individuals.size() + ") extracted in "
					+ (float) (System.currentTimeMillis() - time) / 1000 + " seconds.");
			
		}
		catch (OWLOntologyCreationException e)
		{
			System.err.println("Impossible to load " + university.getAbsolutePath());
			e.printStackTrace();
		}
		
	}
	
}