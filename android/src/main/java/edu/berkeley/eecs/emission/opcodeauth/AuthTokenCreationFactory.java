package edu.berkeley.eecs.emission.opcodeauth;

import android.content.Context;

/**
 * Created by shankari on 8/19/17.
 *
 * Factory to generate AuthTokenCreator instances based on the connection settings
 */

public class AuthTokenCreationFactory {
    public static AuthTokenCreator getInstance(Context ctxt) {
        return new PromptedAuth(ctxt);
    }
}
