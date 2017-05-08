package org.freelectron.leobel.testlwa;

import org.freelectron.leobel.testlwa.models.message.Directive;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by leobel on 2/17/17.
 */

@Singleton
@Component(modules = {AppModule.class, ServicesModule.class})
public interface ApplicationComponent {
    void inject(BaseActivity activity);
    void inject(MainActivity activity);
    void inject(ThingToTryActivity activity);
    void inject(AlexaSpeechRecognizerActivity activity);

    void inject(Directive message);

}
