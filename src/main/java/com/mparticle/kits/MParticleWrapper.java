package com.mparticle.kits;

import com.mparticle.MParticle;

/**
 * Wrapper class for {@link MParticle} to allow for testing static methods
 */
public class MParticleWrapper {

    public boolean isDebuggingEnvironment() {
        return MParticle.getInstance().getEnvironment() == MParticle.Environment.Development;
    }
}
