package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;

public abstract class GetRequest  <T extends BaseModel<BaseModel>> {
    protected RequestSpecification requestSpecification;// как отправлять
    protected ResponseSpecification responseSpecification;// что ожидать в ответ

    public GetRequest(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public abstract ValidatableResponse get();
}

