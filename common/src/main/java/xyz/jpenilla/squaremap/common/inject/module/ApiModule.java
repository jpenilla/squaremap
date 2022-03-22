package xyz.jpenilla.squaremap.common.inject.module;

import com.google.inject.AbstractModule;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.common.SquaremapApiProvider;

public final class ApiModule extends AbstractModule {
    @Override
    protected void configure() {
        this.bind(Squaremap.class)
            .to(SquaremapApiProvider.class);
    }
}
