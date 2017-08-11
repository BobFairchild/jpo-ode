package us.dot.its.jpo.ode.upload;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;

import mockit.Capturing;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.importer.ImporterDirectoryWatcher;
import us.dot.its.jpo.ode.storage.StorageFileNotFoundException;
import us.dot.its.jpo.ode.storage.StorageService;

public class FileUploadControllerTest {

   FileUploadController testFileUploadController;

   @Mocked
   StorageService mockStorageService;

   @Injectable
   OdeProperties injectableOdeProperties;

   @Injectable
   SimpMessagingTemplate injectableSimpMessagingTemplate;

   @Capturing
   Executors capturingExecutors;
   @Capturing
   ImporterDirectoryWatcher capturingImporterDirectoryWatcher;
   @Mocked
   ExecutorService mockExecutorService;

   @Mocked
   OdeProperties mockOdeProperties;

   @Mocked
   MultipartFile mockMultipartFile;

   @Before
   public void constructorShouldLaunchFourThreads() {
      new Expectations() {
         {
            mockOdeProperties.getUploadLocationRoot();
            result = "testRootDir";
            mockOdeProperties.getUploadLocationBsm();
            result = "testBsmDir";
            mockOdeProperties.getUploadLocationMessageFrame();
            result = "testMessageFrameDir";

            Executors.newCachedThreadPool();
            result = mockExecutorService;

            mockExecutorService.submit((Runnable) any);
            times = 4;
         }
      };
      testFileUploadController = new FileUploadController(mockStorageService, mockOdeProperties,
            injectableSimpMessagingTemplate);
   }

   @Test
   public void handleFileUploadReturnsErrorOnStorageException() {
      new Expectations() {
         {
            mockStorageService.store((MultipartFile) any, anyString);
            result = new StorageFileNotFoundException("testException123");
         }
      };

      assertEquals(HttpStatus.BAD_REQUEST,
            testFileUploadController.handleFileUpload(mockMultipartFile, "type").getStatusCode());
   }

   @Test
   public void successfulUploadReturnsHttpOk() {
      new Expectations() {
         {
            mockStorageService.store((MultipartFile) any, anyString);
            times = 1;
         }
      };

      assertEquals(HttpStatus.OK, testFileUploadController.handleFileUpload(mockMultipartFile, "type").getStatusCode());
   }

   @Test
   public void testStorageFileNotFoundException() {
      assertEquals(HttpStatus.NOT_FOUND, testFileUploadController
            .handleStorageFileNotFound(new StorageFileNotFoundException("testException123")).getStatusCode());
   }
}
