package org.recap.service.accession;

import org.apache.camel.Exchange;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCaseUT;
import org.recap.ScsbConstants;
import org.recap.model.accession.AccessionModelRequest;
import org.recap.model.accession.AccessionRequest;
import org.recap.model.accession.AccessionResponse;
import org.recap.model.accession.AccessionSummary;
import org.recap.model.jpa.AccessionEntity;
import org.recap.repository.jpa.AccessionDetailsRepository;
import org.recap.service.accession.callable.BibDataCallable;
import org.recap.util.AccessionProcessService;
import org.recap.util.AccessionUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class BulkAccessionServiceUT extends BaseTestCaseUT {

    @InjectMocks
    BulkAccessionService bulkAccessionService;

    @Mock
    Exchange exchange;


    @Mock
    AccessionProcessService accessionProcessService;

    @Mock
    AccessionValidationService accessionValidationService;

    @Mock
    AccessionDetailsRepository accessionDetailsRepository;

    @Mock
    AccessionValidationService.AccessionValidationResponse accessionValidationResponse;

    @Mock
    AccessionUtil accessionUtil;

    @Mock
    ApplicationContext applicationContext;

    @Test
    public void doBulkAccession() {
        List<AccessionRequest> accessionRequestList=getAccessionRequests();
        AccessionSummary accessionSummary=new AccessionSummary("test");
        Mockito.when(accessionProcessService.removeDuplicateRecord(Mockito.anyList())).thenReturn(removeDuplicateRecord(accessionRequestList));
        ReflectionTestUtils.setField(bulkAccessionService,"batchAccessionThreadSize",20);
        Mockito.when(accessionValidationService.validateBarcodeOrCustomerCode(Mockito.anyString(),Mockito.anyString(), Mockito.anyString())).thenReturn(accessionValidationResponse);
        Mockito.when(accessionValidationResponse.getOwningInstitution()).thenReturn("PUL");
        Mockito.when(accessionValidationResponse.getMessage()).thenReturn(ScsbConstants.ITEM_ALREADY_ACCESSIONED);
        Mockito.when(accessionValidationResponse.isValid()).thenReturn(true);
        BibDataCallable bibDataCallable = Mockito.mock(BibDataCallable.class);
        Mockito.when(applicationContext.getBean(BibDataCallable.class)).thenReturn(bibDataCallable);
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(accessionRequestList);
        accessionModelRequest.setImsLocationCode("test");
        Mockito.when(accessionValidationService.validateImsLocationCode(Mockito.anyString())).thenReturn(accessionValidationResponse);
        List<AccessionResponse> response=bulkAccessionService.doAccession(accessionModelRequest,accessionSummary,exchange);
        assertNull(response);
    }

    @Test
    public void doBulkAccessionInvalid() {
        List<AccessionRequest> accessionRequestList=getAccessionRequests();
        AccessionSummary accessionSummary=new AccessionSummary("test");
        Mockito.when(accessionProcessService.removeDuplicateRecord(Mockito.anyList())).thenReturn(removeDuplicateRecord(accessionRequestList));
        ReflectionTestUtils.setField(bulkAccessionService,"batchAccessionThreadSize",20);
        Mockito.when(accessionValidationService.validateBarcodeOrCustomerCode(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(accessionValidationResponse);
        Mockito.when(accessionValidationResponse.getOwningInstitution()).thenReturn("PUL");
        Mockito.when(accessionValidationResponse.isValid()).thenReturn(true).thenReturn(false);
        Mockito.when(accessionValidationResponse.getMessage()).thenReturn(ScsbConstants.ITEM_ALREADY_ACCESSIONED);
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(accessionRequestList);
        accessionModelRequest.setImsLocationCode("test");
        BibDataCallable bibDataCallable = Mockito.mock(BibDataCallable.class);
        Mockito.when(applicationContext.getBean(BibDataCallable.class)).thenReturn(bibDataCallable);

        Mockito.when(accessionValidationService.validateImsLocationCode(Mockito.anyString())).thenReturn(accessionValidationResponse);
        List<AccessionResponse> response=bulkAccessionService.doAccession(accessionModelRequest,accessionSummary,exchange);
        assertNull(response);
    }

    @Test
    public void doBulkAccessionInvalidImsLocation() {
        List<AccessionRequest> accessionRequestList=getAccessionRequests();
        AccessionSummary accessionSummary=new AccessionSummary("test");
        Mockito.when(accessionProcessService.removeDuplicateRecord(Mockito.anyList())).thenReturn(removeDuplicateRecord(accessionRequestList));
        ReflectionTestUtils.setField(bulkAccessionService,"batchAccessionThreadSize",20);
        Mockito.when(accessionValidationService.validateBarcodeOrCustomerCode(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(accessionValidationResponse);
        Mockito.when(accessionValidationResponse.getOwningInstitution()).thenReturn("PUL");
        Mockito.when(accessionValidationResponse.getMessage()).thenReturn(ScsbConstants.ITEM_ALREADY_ACCESSIONED);
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(accessionRequestList);
        accessionModelRequest.setImsLocationCode("test");
        Mockito.when(accessionValidationService.validateImsLocationCode(Mockito.anyString())).thenReturn(accessionValidationResponse);
        List<AccessionResponse> response=bulkAccessionService.doAccession(accessionModelRequest,accessionSummary,exchange);
        assertNull(response);
    }


    @Test
    public void saveRequest() {
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(getAccessionRequests());
        String status=bulkAccessionService.saveRequest(accessionModelRequest);
        assertEquals(ScsbConstants.ACCESSION_SAVE_SUCCESS_STATUS,status);
    }

    @Test
    public void saveRequestException() {
        AccessionModelRequest accessionModelRequest=new AccessionModelRequest();
        accessionModelRequest.setAccessionRequests(getAccessionRequests());
        Mockito.when(accessionDetailsRepository.save(Mockito.any())).thenThrow(NullPointerException.class);
        String status=bulkAccessionService.saveRequest(accessionModelRequest);
        assertTrue(status.contains(ScsbConstants.ACCESSION_SAVE_FAILURE_STATUS));
    }

    @Test
    public void getAccessionRequestException() {
        Mockito.when(accessionDetailsRepository.findByAccessionStatus(Mockito.anyString())).thenReturn(getAccessionEntities("pending"));
        List<AccessionEntity> accessionEntities=bulkAccessionService.getAccessionEntities(ScsbConstants.PENDING);
        List<AccessionModelRequest> accessionModelRequestList=new ArrayList<>();
        List<AccessionRequest> accessionEntity1=bulkAccessionService.getAccessionRequest(accessionModelRequestList);
        assertNotNull(accessionEntity1);
        assertEquals(java.util.Optional.ofNullable(1), java.util.Optional.ofNullable(accessionEntities.get(0).getId()));
    }

    @Test
    public void getAccessionRequest() {
        Mockito.when(accessionDetailsRepository.findByAccessionStatus(Mockito.anyString())).thenReturn(getAccessionEntities("pending"));
        bulkAccessionService.updateStatusForAccessionEntities(getAccessionEntities("[{\"customerCode\":\"PA\",\"itemBarcode\":\"123\"}]"), ScsbConstants.PENDING);
        List<AccessionModelRequest> accessionModelRequestList=new ArrayList<>();
        List<AccessionRequest> accessionEntity1=bulkAccessionService.getAccessionRequest(accessionModelRequestList);
        assertNotNull(accessionEntity1);
   }

    private List<AccessionEntity> getAccessionEntities(String request) {
        List<AccessionEntity> accessionEntityList=new ArrayList<>();
        AccessionEntity accessionEntity=new AccessionEntity();
        accessionEntity.setAccessionRequest(request);
        accessionEntity.setAccessionStatus(ScsbConstants.PENDING);
        accessionEntity.setCreatedDate(new Date());
        accessionEntity.setId(1);
        accessionEntityList.add(accessionEntity);
        return accessionEntityList;
    }

    public List<AccessionRequest> removeDuplicateRecord(List<AccessionRequest> trimmedAccessionRequests) {
        Set<AccessionRequest> accessionRequests = new HashSet<>(trimmedAccessionRequests);
        return new ArrayList<>(accessionRequests);
    }

    private List<AccessionRequest> getAccessionRequests() {
        List<AccessionRequest> accessionRequestList = new ArrayList<>();
        AccessionRequest accessionRequest = new AccessionRequest();
        accessionRequest.setCustomerCode("PA");
        accessionRequest.setItemBarcode("32101095533293");
        accessionRequestList.add(accessionRequest);
        return accessionRequestList;
    }
}
