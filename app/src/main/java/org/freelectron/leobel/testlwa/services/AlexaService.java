package org.freelectron.leobel.testlwa.services;

import org.freelectron.leobel.testlwa.models.Response;

import java.util.List;

import rx.Observable;

/**
 * Created by leobel on 2/20/17.
 */

public interface AlexaService {
    Observable<Response<okhttp3.Response>> establishDownChanelStream();

    Observable<Response<Void>> expectSpeechTimedOutEvent();
}
