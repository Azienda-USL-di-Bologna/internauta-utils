package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

/**
 *
 * @author boria
 */
public class MetadatiBuilder {

    private VersamentoBuilder versamentoBuilder = new VersamentoBuilder();

    public VersamentoBuilder build() {
        versamentoBuilder.setDocType("ciao");
        return versamentoBuilder;
    }

}
