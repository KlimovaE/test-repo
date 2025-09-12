package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UpdateCustomerProfileRequest;
import spec.ResponseSpecs;

import static io.restassured.RestAssured.given;

public class UpdateCustomerProfile extends PutRequest<UpdateCustomerProfileRequest> {

    // Конструктор без параметров для ResponseSpecification
    public UpdateCustomerProfile(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    // Конструктор С параметрами для динамических проверок
    public UpdateCustomerProfile(RequestSpecification requestSpec, String expectedName) {
        super(requestSpec, ResponseSpecs.requestReturnOkAndCheckNewName(expectedName));
    }

    // Реализация PUT (основной метод для обновления профиля)
    @Override
    public ValidatableResponse put(UpdateCustomerProfileRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put("/api/v1/customer/profile")  // PUT запрос
                .then()
                .spec(responseSpecification);
    }

    // Дополнительный метод PUT с кастомной проверкой
    public ValidatableResponse put(UpdateCustomerProfileRequest model, String expectedName) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put("/api/v1/customer/profile")
                .then()
                .spec(ResponseSpecs.requestReturnOkAndCheckNewName(expectedName));
    }


}
