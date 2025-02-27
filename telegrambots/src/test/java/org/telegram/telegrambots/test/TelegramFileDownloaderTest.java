package org.telegram.telegrambots.test;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.facilities.filedownloader.TelegramFileDownloader;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.updateshandlers.DownloadFileCallback;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramFileDownloaderTest {

    private TelegramFileDownloader telegramFileDownloader;

    @Mock
    private DownloadFileCallback<String> downloadFileCallbackMock;

    @Mock
    private HttpClient httpClientMock;

    @Mock
    private ClassicHttpResponse httpResponseMock;

    @Mock
    private HttpEntity httpEntityMock;

    private Supplier<String> tokenSupplierMock = () -> "someToken";

    @BeforeEach
    void setup() throws IOException {
        when(httpResponseMock.getCode()).thenReturn(200);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(toInputStream("Some File Content", defaultCharset()));
        when(httpClientMock.execute(any(HttpUriRequest.class), any(HttpClientResponseHandler.class))).thenReturn(httpResponseMock);

        telegramFileDownloader = new TelegramFileDownloader(httpClientMock, tokenSupplierMock);
    }

    @Test
    void testFileDownload() throws TelegramApiException, IOException {
        File returnFile = telegramFileDownloader.downloadFile("someFilePath");
        String content = readFileToString(returnFile, defaultCharset());

        assertEquals("Some File Content", content);
    }

    @Test
    void testDownloadException() {
        when(httpResponseMock.getCode()).thenReturn(500);

        Assertions.assertThrows(TelegramApiException.class,
                () -> telegramFileDownloader.downloadFile("someFilePath"));
    }

    @Test
    void testAsyncDownload() throws TelegramApiException, IOException {
        final ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        telegramFileDownloader.downloadFileAsync("someFilePath", downloadFileCallbackMock);

        verify(downloadFileCallbackMock, timeout(100)
                .times(1))
                .onResult(any(), fileArgumentCaptor.capture());

        String content = readFileToString(fileArgumentCaptor.getValue(), defaultCharset());
        assertEquals("Some File Content", content);
    }

    @Test
    void testAsyncException() throws TelegramApiException {
        final ArgumentCaptor<Exception> exceptionArgumentCaptor = ArgumentCaptor.forClass(Exception.class);

        when(httpResponseMock.getCode()).thenReturn(500);

        telegramFileDownloader.downloadFileAsync("someFilePath", downloadFileCallbackMock);

        verify(downloadFileCallbackMock, timeout(100)
                .times(1))
                .onException(any(), exceptionArgumentCaptor.capture());

        Exception e = exceptionArgumentCaptor.getValue();
        assertThat(e, instanceOf(TelegramApiException.class));
        assertEquals(e.getCause().getCause().getMessage(), "Unexpected Status code while downloading file. Expected 200 got 500");
    }

}
