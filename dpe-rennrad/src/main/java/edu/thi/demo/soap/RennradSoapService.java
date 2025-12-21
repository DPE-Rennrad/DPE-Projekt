package edu.thi.demo.soap;

import edu.thi.demo.model.Rennrad;
import edu.thi.demo.service.RennradService;
import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import java.util.List;

@WebService
public class RennradSoapService {

    @Inject
    RennradService rennradService;

    @WebMethod
    public List<Rennrad> getAllRennraeder() {
        return rennradService.getAllRennraeder();
    }
}