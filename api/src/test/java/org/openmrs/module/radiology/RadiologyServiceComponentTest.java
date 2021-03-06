/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.radiology;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Tests {@link RadiologyService}
 */
public class RadiologyServiceComponentTest extends BaseModuleContextSensitiveTest {
	
	private static final String STUDIES_TEST_DATASET = "org/openmrs/module/radiology/include/RadiologyServiceComponentTestDataset.xml";
	
	private static final int PATIENT_ID_WITH_ONLY_ONE_NON_RADIOLOGY_ORDER = 70011;
	
	private static final int PATIENT_ID_WITH_TWO_STUDIES_AND_NO_NON_RADIOLOGY_ORDER = 70021;
	
	private static final int PATIENT_ID_WITH_TWO_RADIOLOGY_ORDERS = 70021;
	
	private static final int PATIENT_ID_WITH_ONE_RADIOLOGY_ORDER = 70022;
	
	private static final int RADIOLOGY_ORDER_ID_WITH_ONE_OBS = 2002;
	
	private static final int RADIOLOGY_ORDER_ID_WITHOUT_OBS = 2001;
	
	private static final int RADIOLOGY_ORDER_ID_WITHOUT_STUDY = 2004;
	
	private static final int EXISTING_RADIOLOGY_ORDER_ID = 2001;
	
	private static final int NON_EXISTING_RADIOLOGY_ORDER_ID = 99999;
	
	private static final String EXISTING_STUDY_INSTANCE_UID = "1.2.826.0.1.3680043.8.2186.1.1";
	
	private static final String NON_EXISTING_STUDY_INSTANCE_UID = "1.2.826.0.1.3680043.8.2186.1.9999";
	
	private static final int EXISTING_STUDY_ID = 1;
	
	private static final int NON_EXISTING_STUDY_ID = 99999;
	
	private static final int CONCEPT_ID_FOR_FRACTURE = 178;
	
	private static final int TOTAL_NUMBER_OF_RADIOLOGY_ORDERS = 3;
	
	private PatientService patientService = null;
	
	private ConceptService conceptService = null;
	
	private AdministrationService administrationService = null;
	
	private EncounterService encounterService = null;
	
	private ProviderService providerService = null;
	
	private OrderService orderService = null;
	
	private RadiologyService radiologyService = null;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	/**
	 * Overriding following method is necessary to enable MVCC which is disabled by default in DB h2
	 * used for the component tests. This prevents following exception:
	 * org.hibernate.exception.GenericJDBCException: could not load an entity:
	 * [org.openmrs.GlobalProperty#order.nextOrderNumberSeed] due to
	 * "Timeout trying to lock table "GLOBAL_PROPERTY"; SQL statement:" which occurs in all tests
	 * touching methods that call orderService.saveOrder()
	 */
	@Override
	public Properties getRuntimeProperties() {
		Properties result = super.getRuntimeProperties();
		String url = result.getProperty(Environment.URL);
		if (url.contains("jdbc:h2:") && !url.contains(";MVCC=TRUE")) {
			result.setProperty(Environment.URL, url + ";MVCC=TRUE");
		}
		return result;
	}
	
	@Before
	public void runBeforeAllTests() throws Exception {
		
		if (patientService == null) {
			patientService = Context.getPatientService();
		}
		
		if (conceptService == null) {
			conceptService = Context.getConceptService();
		}
		
		if (administrationService == null) {
			administrationService = Context.getAdministrationService();
		}
		
		if (encounterService == null) {
			encounterService = Context.getEncounterService();
		}
		
		if (providerService == null) {
			providerService = Context.getProviderService();
		}
		
		if (orderService == null) {
			orderService = Context.getOrderService();
		}
		
		if (radiologyService == null) {
			radiologyService = Context.getService(RadiologyService.class);
		}
		
		executeDataSet(STUDIES_TEST_DATASET);
	}
	
	/**
	 * @see RadiologyService#placeRadiologyOrder(RadiologyOrder)
	 * @verifies create new radiology order and study from given radiology order object
	 */
	@Test
	public void placeRadiologyOrder_shouldCreateNewRadiologyOrderAndStudyGivenRadiologyOrderObject() throws Exception {
		
		RadiologyOrder radiologyOrder = getUnsavedRadiologyOrder();
		
		radiologyOrder = radiologyService.placeRadiologyOrder(radiologyOrder);
		
		assertNotNull(radiologyOrder);
		assertNotNull(radiologyOrder.getOrderId());
		assertNotNull(radiologyOrder.getStudy());
		assertNotNull(radiologyOrder.getStudy().getStudyId());
	}
	
	/**
	 * Convenience method to get a RadiologyOrder object with all required values filled in but
	 * which is not yet saved in the database
	 * 
	 * @return RadiologyOrder object that can be saved to the database
	 */
	public RadiologyOrder getUnsavedRadiologyOrder() {
		
		RadiologyOrder radiologyOrder = new RadiologyOrder();
		
		radiologyOrder.setPatient(patientService.getPatient(PATIENT_ID_WITH_ONLY_ONE_NON_RADIOLOGY_ORDER));
		radiologyOrder.setOrderer(providerService.getProviderByIdentifier("1"));
		radiologyOrder.setConcept(conceptService.getConcept(CONCEPT_ID_FOR_FRACTURE));
		radiologyOrder.setInstructions("CT ABDOMEN PANCREAS WITH IV CONTRAST");
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(2015, Calendar.FEBRUARY, 4, 14, 35, 0);
		radiologyOrder.setScheduledDate(calendar.getTime());
		radiologyOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		
		Study study = new Study();
		study.setModality(Modality.CT);
		study.setMwlStatus(MwlStatus.DEFAULT);
		study.setScheduledStatus(ScheduledProcedureStepStatus.SCHEDULED);
		radiologyOrder.setStudy(study);
		
		return radiologyOrder;
	}
	
	/**
	 * @see RadiologyService#placeRadiologyOrder(RadiologyOrder)
	 * @verifies throw illegal argument exception given null
	 */
	@Test
	public void placeRadiologyOrder_shouldThrowIllegalArgumentExceptionGivenNull() throws Exception {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("radiologyOrder is required");
		radiologyService.placeRadiologyOrder(null);
	}
	
	/**
	 * @see RadiologyService#placeRadiologyOrder(RadiologyOrder)
	 * @verifies throw illegal argument exception given existing radiology order
	 */
	@Test
	public void placeRadiologyOrder_shouldThrowIllegalArgumentExceptionGivenExistingRadiologyOrder() throws Exception {
		
		RadiologyOrder radiologyOrder = getUnsavedRadiologyOrder();
		
		radiologyOrder = radiologyService.placeRadiologyOrder(radiologyOrder);
		
		assertNotNull(radiologyOrder);
		assertNotNull(radiologyOrder.getOrderId());
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Cannot edit an existing order!");
		radiologyService.placeRadiologyOrder(radiologyOrder);
	}
	
	/**
	 * @see RadiologyService#placeRadiologyOrder(RadiologyOrder)
	 * @verifies throw illegal argument exception if given radiology order has no study
	 */
	@Test
	public void placeRadiologyOrder_shouldThrowIllegalArgumentExceptionIfGivenRadiologyOrderHasNoStudy() throws Exception {
		
		RadiologyOrder radiologyOrder = getUnsavedRadiologyOrder();
		radiologyOrder.setStudy(null);
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("radiologyOrder.study is required");
		radiologyService.placeRadiologyOrder(radiologyOrder);
	}
	
	/**
	 * @see RadiologyService#placeRadiologyOrder(RadiologyOrder)
	 * @verifies throw illegal argument exception if given study modality is null
	 */
	@Test
	public void placeRadiologyOrder_shouldThrowIllegalArgumentExceptionIfGivenStudyModalityIsNull() throws Exception {
		
		RadiologyOrder radiologyOrder = getUnsavedRadiologyOrder();
		radiologyOrder.getStudy().setModality(null);
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("radiologyOrder.study.modality is required");
		radiologyService.placeRadiologyOrder(radiologyOrder);
	}
	
	/**
	 * @see RadiologyService#discontinueRadiologyOrder(RadiologyOrder, Provider, Date, String)
	 * @verifies should create discontinuation order which discontinues given radiology order object
	 */
	@Test
	public void discontinueRadiologyOrder_shouldCreateDiscontinuationOrderWhichDiscontinuesGivenRadiologyOrderObject()
	        throws Exception {
		
		RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByOrderId(EXISTING_RADIOLOGY_ORDER_ID);
		String discontinueReason = "Wrong Procedure";
		Date discontinueDate = new GregorianCalendar(2015, Calendar.JANUARY, 01).getTime();
		
		Order discontinuationOrder = radiologyService.discontinueRadiologyOrder(radiologyOrder, radiologyOrder.getOrderer(),
		    discontinueDate, discontinueReason);
		
		assertNotNull(discontinuationOrder);
		assertThat(discontinuationOrder.getAction(), is(Order.Action.DISCONTINUE));
		
		assertThat(discontinuationOrder.getPreviousOrder(), is((Order) radiologyOrder));
		assertThat(radiologyOrder.isActive(), is(false));
	}
	
	/**
	 * @see RadiologyService#discontinueRadiologyOrder(RadiologyOrder, Provider, Date, String)
	 * @verifies should throw illegal argument exception given empty radiology order
	 */
	@Test
	public void discontinueRadiologyOrder_shouldThrowIllegalArgumentExceptionGivenEmptyRadiologyOrder() throws Exception {
		
		RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByOrderId(EXISTING_RADIOLOGY_ORDER_ID);
		String discontinueReason = "Wrong Procedure";
		Date discontinueDate = new GregorianCalendar(2015, Calendar.JANUARY, 01).getTime();
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("radiologyOrder is required");
		radiologyService.discontinueRadiologyOrder(null, radiologyOrder.getOrderer(), discontinueDate, discontinueReason);
	}
	
	/**
	 * @see RadiologyService#discontinueRadiologyOrder(RadiologyOrder, Provider, Date, String)
	 * @verifies should throw illegal argument exception given radiology order with orderId null
	 */
	@Test
	public void discontinueRadiologyOrder_shouldThrowIllegalArgumentExceptionGivenRadiologyOrderWithOrderIdNull()
	        throws Exception {
		
		RadiologyOrder radiologyOrder = getUnsavedRadiologyOrder();
		String discontinueReason = "Wrong Procedure";
		Date discontinueDate = new GregorianCalendar(2015, Calendar.JANUARY, 01).getTime();
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("orderId is null");
		radiologyService.discontinueRadiologyOrder(radiologyOrder, radiologyOrder.getOrderer(), discontinueDate,
		    discontinueReason);
	}
	
	/**
	 * @see RadiologyService#discontinueRadiologyOrder(RadiologyOrder, Provider, Date, String)
	 * @verifies should throw illegal argument exception if radiology order is not active
	 */
	@Test
	public void discontinueRadiologyOrder_shouldThrowIllegalArgumentExceptionIfRadiologyOrderIsNotActive() throws Exception {
		
		RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByOrderId(EXISTING_RADIOLOGY_ORDER_ID);
		radiologyOrder.setAction(Order.Action.DISCONTINUE);
		String discontinueReason = "Wrong Procedure";
		Date discontinueDate = new GregorianCalendar(2015, Calendar.JANUARY, 01).getTime();
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("order is not active");
		radiologyService.discontinueRadiologyOrder(radiologyOrder, radiologyOrder.getOrderer(), discontinueDate,
		    discontinueReason);
	}
	
	/**
	 * @see RadiologyService#discontinueRadiologyOrder(RadiologyOrder, Provider, Date, String)
	 * @verifies should throw illegal argument exception given empty provider
	 */
	@Test
	public void discontinueRadiologyOrder_shouldThrowIllegalArgumentExceptionGivenEmptyProvider() throws Exception {
		
		RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByOrderId(EXISTING_RADIOLOGY_ORDER_ID);
		String discontinueReason = "Wrong Procedure";
		Date discontinueDate = new GregorianCalendar(2015, Calendar.JANUARY, 01).getTime();
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("provider is required");
		radiologyService.discontinueRadiologyOrder(radiologyOrder, null, discontinueDate, discontinueReason);
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrderByOrderId(Integer)
	 * @verifies should return radiology order matching order id
	 */
	@Test
	public void getRadiologyOrderByOrderId_shouldReturnRadiologyOrderMatchingOrderId() {
		
		RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByOrderId(EXISTING_RADIOLOGY_ORDER_ID);
		
		assertNotNull(radiologyOrder);
		assertThat(radiologyOrder.getOrderId(), is(EXISTING_RADIOLOGY_ORDER_ID));
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrderByOrderId(Integer)
	 * @verifies should return null if no match was found
	 */
	@Test
	public void getRadiologyOrderByOrderId_shouldReturnNullIfNoMatchIsFound() {
		
		RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByOrderId(NON_EXISTING_RADIOLOGY_ORDER_ID);
		
		assertNull(radiologyOrder);
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrderByOrderId(Integer)
	 * @verifies should throw illegal argument exception given null
	 */
	@Test
	public void getRadiologyOrderByOrderId_shouldThrowIllegalArgumentExceptionGivenNull() {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("orderId is required");
		radiologyService.getRadiologyOrderByOrderId(null);
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrdersByPatient(Patient)
	 * @verifies should return all radiology orders associated with given patient
	 */
	@Test
	public void getRadiologyOrdersByPatient_shouldReturnAllRadiologyOrdersAssociatedWithGivenPatient() {
		
		Patient patientWithTwoRadiologyOrders = patientService
		        .getPatient(PATIENT_ID_WITH_TWO_STUDIES_AND_NO_NON_RADIOLOGY_ORDER);
		
		List<RadiologyOrder> radiologyOrders = radiologyService.getRadiologyOrdersByPatient(patientWithTwoRadiologyOrders);
		
		assertThat(radiologyOrders.size(), is(2));
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrdersByPatient(Patient)
	 * @verifies should return empty list given patient without associated radiology orders
	 */
	@Test
	public void getRadiologyOrdersByPatient_shouldReturnEmptyListGivenPatientWithoutAssociatedRadiologyOrders() {
		
		Patient patientWithoutRadiologyOrders = patientService.getPatient(PATIENT_ID_WITH_ONLY_ONE_NON_RADIOLOGY_ORDER);
		
		List<RadiologyOrder> radiologyOrders = radiologyService.getRadiologyOrdersByPatient(patientWithoutRadiologyOrders);
		
		assertThat(radiologyOrders.size(), is(0));
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrdersByPatient(Patient)
	 * @verifies should throw illegal argument exception given null
	 */
	@Test
	public void getRadiologyOrdersByPatient_shouldThrowIllegalArgumentExceptionGivenNull() {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("patient is required");
		radiologyService.getRadiologyOrdersByPatient(null);
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrdersByPatients(List<Patient>)
	 * @verifies should return all radiology orders associated with given patients
	 */
	@Test
	public void getRadiologyOrdersByPatients_shouldReturnAllRadiologyOrdersAssociatedWithGivenPatients() {
		
		Patient patientWithTwoRadiologyOrders = patientService.getPatient(PATIENT_ID_WITH_TWO_RADIOLOGY_ORDERS);
		Patient patientWithOneRadiologyOrder = patientService.getPatient(PATIENT_ID_WITH_ONE_RADIOLOGY_ORDER);
		List<Patient> patientsWithThreeRadiologyOrders = new ArrayList<Patient>();
		patientsWithThreeRadiologyOrders.add(patientWithTwoRadiologyOrders);
		patientsWithThreeRadiologyOrders.add(patientWithOneRadiologyOrder);
		
		List<RadiologyOrder> radiologyOrders = radiologyService
		        .getRadiologyOrdersByPatients(patientsWithThreeRadiologyOrders);
		
		assertThat(radiologyOrders.size(), is(3));
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrdersByPatients(List<Patient>)
	 * @verifies should return all radiology orders given empty patient list
	 */
	@Test
	public void getRadiologyOrdersByPatients_shouldReturnAllRadiologyOrdersGivenEmptyPatientList() {
		
		List<Patient> emptyPatientList = new ArrayList<Patient>();
		
		List<RadiologyOrder> radiologyOrders = radiologyService.getRadiologyOrdersByPatients(emptyPatientList);
		
		assertThat(radiologyOrders.size(), is(TOTAL_NUMBER_OF_RADIOLOGY_ORDERS));
	}
	
	/**
	 * @see RadiologyService#getRadiologyOrdersByPatients(List<Patient>)
	 * @verifies should throw illegal argument exception given null
	 */
	@Test
	public void getRadiologyOrdersByPatients_shouldThrowIllegalArgumentExceptionGivenNull() {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("patients is required");
		radiologyService.getRadiologyOrdersByPatients(null);
	}
	
	/**
	 * @see RadiologyService#getStudyByStudyId(Integer)
	 * @verifies should return study for given study id
	 */
	@Test
	public void getStudyByStudyId_shouldReturnStudyForGivenStudyId() throws Exception {
		
		Study study = radiologyService.getStudyByStudyId(EXISTING_STUDY_ID);
		
		assertNotNull(study);
		assertThat(study.getRadiologyOrder().getOrderId(), is(EXISTING_RADIOLOGY_ORDER_ID));
	}
	
	/**
	 * @see RadiologyService#getStudyByStudyId(Integer)
	 * @verifies should return null if no match was found
	 */
	@Test
	public void getStudyByStudyId_shouldReturnNullIfNoMatchIsFound() throws Exception {
		
		Study study = radiologyService.getStudyByStudyId(NON_EXISTING_STUDY_ID);
		
		assertNull(study);
	}
	
	/**
	 * @see RadiologyService#getStudyByOrderId(Integer)
	 * @verifies should return study associated with radiology order for which order id is given
	 */
	@Test
	public void getStudyByOrderId_shouldReturnStudyMatching() throws Exception {
		
		Study study = radiologyService.getStudyByOrderId(EXISTING_RADIOLOGY_ORDER_ID);
		
		assertNotNull(study);
		assertThat(study.getRadiologyOrder().getOrderId(), is(EXISTING_RADIOLOGY_ORDER_ID));
	}
	
	/**
	 * @see RadiologyService#getStudyByOrderId(Integer)
	 * @verifies should return null if no match was found
	 */
	@Test
	public void getStudyByOrderId_shouldReturnNullIfNoMatchIsFound() {
		
		Study study = radiologyService.getStudyByOrderId(NON_EXISTING_RADIOLOGY_ORDER_ID);
		
		assertNull(study);
	}
	
	/**
	 * @see RadiologyService#getStudyByOrderId(Integer)
	 * @verifies should throw illegal argument exception given null
	 */
	@Test
	public void getStudyByOrderId_shouldThrowIllegalArgumentExceptionGivenNull() {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("orderId is required");
		radiologyService.getStudyByOrderId(null);
	}
	
	/**
	 * @see RadiologyService#getStudyByStudyInstanceUid(String)
	 * @verifies should return study matching study instance uid
	 */
	@Test
	public void getStudyByStudyInstanceUid_shouldReturnStudyMatchingUid() throws Exception {
		Study study = radiologyService.getStudyByStudyInstanceUid(EXISTING_STUDY_INSTANCE_UID);
		
		assertNotNull(study);
		assertThat(study.getStudyInstanceUid(), is(EXISTING_STUDY_INSTANCE_UID));
	}
	
	/**
	 * @see RadiologyService#getStudyByStudyInstanceUid(String)
	 * @verifies should return null if no match was found
	 */
	@Test
	public void getStudyByStudyInstanceUid_shouldReturnNullIfNoMatchIsFound() throws Exception {
		Study study = radiologyService.getStudyByStudyInstanceUid(NON_EXISTING_STUDY_INSTANCE_UID);
		
		assertNull(study);
	}
	
	/**
	 * @see RadiologyService#getStudyByStudyInstanceUid(String)
	 * @verifies should throw IllegalArgumentException if study instance uid is null
	 */
	@Test
	public void getStudyByStudyInstanceUid_shouldThrowIllegalArgumentExceptionIfUidIsNull() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("studyInstanceUid is required");
		radiologyService.getStudyByStudyInstanceUid(null);
	}
	
	/**
	 * @see RadiologyService#getStudiesByRadiologyOrders(List<RadiologyOrder>)
	 * @verifies should fetch all studies for given radiology orders
	 */
	@Test
	public void getStudiesByRadiologyOrders_shouldFetchAllStudiesForGivenRadiologyOrders() throws Exception {
		
		Patient patient = patientService.getPatient(PATIENT_ID_WITH_TWO_STUDIES_AND_NO_NON_RADIOLOGY_ORDER);
		List<RadiologyOrder> radiologyOrders = radiologyService.getRadiologyOrdersByPatient(patient);
		
		List<Study> studies = radiologyService.getStudiesByRadiologyOrders(radiologyOrders);
		
		assertThat(studies.size(), is(radiologyOrders.size()));
		assertThat(studies.get(0).getRadiologyOrder(), is(radiologyOrders.get(0)));
		assertThat(studies.get(1).getRadiologyOrder(), is(radiologyOrders.get(1)));
	}
	
	/**
	 * @see RadiologyService#getStudiesByRadiologyOrders(List<RadiologyOrder>)
	 * @verifies should return empty list given radiology orders without associated studies
	 */
	@Test
	public void getStudiesByRadiologyOrders_shouldReturnEmptyListGivenRadiologyOrdersWithoutAssociatedStudies()
	        throws Exception {
		
		RadiologyOrder radiologyOrderWithoutStudy = radiologyService
		        .getRadiologyOrderByOrderId(RADIOLOGY_ORDER_ID_WITHOUT_STUDY);
		List<RadiologyOrder> radiologyOrders = Arrays.asList(radiologyOrderWithoutStudy);
		
		List<Study> studies = radiologyService.getStudiesByRadiologyOrders(radiologyOrders);
		
		assertThat(radiologyOrders.size(), is(1));
		assertThat(studies.size(), is(0));
	}
	
	/**
	 * @see RadiologyService#getStudiesByRadiologyOrders(List<RadiologyOrder>)
	 * @verifies should return empty list given empty radiology order list
	 */
	@Test
	public void getStudiesByRadiologyOrders_shouldReturnEmptyListGivenEmptyRadiologyOrderList() throws Exception {
		
		List<RadiologyOrder> orders = new ArrayList<RadiologyOrder>();
		
		List<Study> studies = radiologyService.getStudiesByRadiologyOrders(orders);
		
		assertThat(orders.size(), is(0));
		assertThat(studies.size(), is(0));
	}
	
	/**
	 * @see RadiologyService#getStudiesByRadiologyOrders(List<RadiologyOrder>)
	 * @verifies should throw IllegalArgumentException given null
	 */
	@Test
	public void getStudiesByRadiologyOrders_shouldThrowIllegalArgumentExceptionGivenNull() throws Exception {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("radiologyOrders are required");
		radiologyService.getStudiesByRadiologyOrders(null);
	}
	
	/**
	 * @see RadiologyService#getObsByOrderId(Integer)
	 * @verifies should fetch all obs for given orderId
	 */
	@Test
	public void getObsByOrderId_shouldFetchAllObsForGivenOrderId() throws Exception {
		List<Obs> obs = radiologyService.getObsByOrderId(RADIOLOGY_ORDER_ID_WITH_ONE_OBS);
		
		assertThat(obs.size(), is(1));
		assertThat(obs.get(0).getOrder().getOrderId(), is(RADIOLOGY_ORDER_ID_WITH_ONE_OBS));
	}
	
	/**
	 * @see RadiologyService#getObsByOrderId(Integer)
	 * @verifies should return empty list given orderId without associated obs
	 */
	@Test
	public void getObsByOrderId_shouldReturnEmptyListGivenOrderIdWithoutAssociatedObs() throws Exception {
		List<Obs> obs = radiologyService.getObsByOrderId(RADIOLOGY_ORDER_ID_WITHOUT_OBS);
		
		assertThat(obs.size(), is(0));
	}
	
	/**
	 * @see RadiologyService#getObsByOrderId(Integer)
	 * @verifies should throw IllegalArgumentException given null
	 */
	@Test
	public void getObsByOrderId_shouldThrowIllegalArgumentExceptionGivenNull() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("orderId is required");
		radiologyService.getObsByOrderId(null);
	}
	
	/**
	 * @see RadiologyService#updateStudyPerformedStatus(String,PerformedProcedureStepStatus)
	 * @verifies update performed status of study associated with given study instance uid
	 */
	@Test
	public void updateStudyPerformedStatus_shouldUpdatePerformedStatusOfStudyAssociatedWithGivenStudyInstanceUid()
	        throws Exception {
		
		Study existingStudy = radiologyService.getStudyByStudyId(EXISTING_STUDY_ID);
		PerformedProcedureStepStatus performedStatusPreUpdate = existingStudy.getPerformedStatus();
		PerformedProcedureStepStatus performedStatusPostUpdate = PerformedProcedureStepStatus.COMPLETED;
		
		Study updatedStudy = radiologyService.updateStudyPerformedStatus(existingStudy.getStudyInstanceUid(),
		    performedStatusPostUpdate);
		
		assertNotNull(updatedStudy);
		assertThat(updatedStudy, is(existingStudy));
		assertThat(performedStatusPreUpdate, is(not(performedStatusPostUpdate)));
		assertThat(updatedStudy.getPerformedStatus(), is(performedStatusPostUpdate));
	}
	
	/**
	 * @see RadiologyService#updateStudyPerformedStatus(String,PerformedProcedureStepStatus)
	 * @verifies throw illegal argument exception if study instance uid is null
	 */
	@Test
	public void updateStudyPerformedStatus_shouldThrowIllegalArgumentExceptionIfStudyInstanceUidIsNull() throws Exception {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("studyInstanceUid is required");
		radiologyService.updateStudyPerformedStatus(null, PerformedProcedureStepStatus.COMPLETED);
	}
	
	/**
	 * @see RadiologyService#updateStudyPerformedStatus(String,PerformedProcedureStepStatus)
	 * @verifies throw illegal argument exception if performed status is null
	 */
	@Test
	public void updateStudyPerformedStatus_shouldThrowIllegalArgumentExceptionIfPerformedStatusIsNull() throws Exception {
		
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("performedStatus is required");
		radiologyService.updateStudyPerformedStatus(EXISTING_STUDY_INSTANCE_UID, null);
	}
}
