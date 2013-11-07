package org.commonjava.maven.galley.maven.model.view;

import org.commonjava.maven.atlas.ident.DependencyScope;

public class PluginDependencyView
    extends DependencyView
{

    private final PluginView plugin;

    public PluginDependencyView( final MavenPomView pomView, final PluginView plugin, final NodeRef element )
    {
        super( pomView, element );
        this.plugin = plugin;
    }

    public PluginView getPlugin()
    {
        return plugin;
    }

    @Override
    public synchronized DependencyScope getScope()
    {
        return DependencyScope.toolchain;
    }

}
