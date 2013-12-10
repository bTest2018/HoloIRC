package com.fusionx.relay.parser;

import com.fusionx.relay.connection.ServerConnection;
import com.fusionx.lightirc.RobolectricGradleTestRunner;
import com.fusionx.relay.Server;
import com.fusionx.relay.ServerConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import android.os.Handler;

@RunWith(RobolectricGradleTestRunner.class)
public class ServerLineParserTest {

    private ServerLineParser parser;

    @Before
    public void setUp() throws Exception {
        final Handler handler = new Handler();
        ServerConfiguration.Builder builder = new ServerConfiguration.Builder();
        final ServerConnection wrapper = new ServerConnection(builder.build(),
                Robolectric.getShadowApplication().getApplicationContext(), handler);
        final Server server = new Server("", wrapper, Robolectric.getShadowApplication()
                .getApplicationContext(), handler);
        parser = new ServerLineParser(server);
    }

    @Test
    public void shouldUpdateResultsWhenButtonIsClicked() throws Exception {
    }
}