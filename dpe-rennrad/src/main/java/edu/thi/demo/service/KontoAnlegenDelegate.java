package edu.thi.demo.service;

import edu.thi.demo.model.Kunde;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

@Named("kontoAnlegenDelegate")
public class KontoAnlegenDelegate implements JavaDelegate {

    @Inject
    KundeService kundeService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String kundeName = (String) execution.getVariable("kundeName");
        String kundeEmail = (String) execution.getVariable("kundeEmail");

        if (kundeName != null) {
            Kunde kunde = new Kunde();
            kunde.name = kundeName;
            kunde.email = kundeEmail != null ? kundeEmail : "";
            kundeService.createKunde(kunde);
            execution.setVariable("kundeId", kunde.id);
        }
    }
}

// Eduard Merker