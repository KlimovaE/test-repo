package iteration_2;

import io.restassured.http.ContentType;
import models.CreateUserRequest;
import models.LoginUserRequest;
import models.UpdateCustomerProfileRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminLoginUserRequest;
import requests.CreateUser;
import requests.UpdateCustomerProfile;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


public class UpdateUserName {
    String adminToken = "Basic YWRtaW46YWRtaW4=";
    String user1Token;
    String user2Token;
    String user1Password = "Kate012!";
    String user2Password = "Kate013!";
    //Генерация уникальных userName для каждого теста
    String user1Username = "A_" + System.currentTimeMillis(); // "kate001_123456"
    String user2Username = "B_" + System.currentTimeMillis(); // "kate002_123456"
    String userRole = "USER";


    @BeforeEach
    public void setupTestData() {
        //создание первого пользователя(middle)
        CreateUserRequest userRequest = CreateUserRequest.builder().
                username(user1Username)
                .password(user1Password)
                .role(userRole)
                .build();
        new CreateUser(RequestSpecs.adminAuthSpec(), ResponseSpecs.entityWasCreated())
                .post(userRequest);

        //Создание второго пользователя
        CreateUserRequest userRequest2 = CreateUserRequest.builder()
                .username(user2Username)
                .password(user2Password)
                .role(userRole)
                .build();
        new CreateUser(RequestSpecs.adminAuthSpec(), ResponseSpecs.entityWasCreated())
                .post(userRequest2);

        //Получение токена для пользователя1
        LoginUserRequest loginUser1Request = LoginUserRequest.builder()
                .username(user1Username)
                .password(user1Password)
                .build();
        user1Token = new AdminLoginUserRequest(RequestSpecs.unAuthSpec(), ResponseSpecs.requestReturnsOK())
                .post(loginUser1Request)
                .extract()
                .header("Authorization");

        //Получение токена для пользователя2
        LoginUserRequest loginUser2Request = LoginUserRequest.builder()
                .username(user2Username)
                .password(user2Password)
                .build();

        user2Token = new AdminLoginUserRequest(RequestSpecs.unAuthSpec(), ResponseSpecs.requestReturnsOK())
                .post(loginUser2Request)
                .extract()
                .header("Authorization");
    }

    public static Stream<Arguments> validNameData() {
        return Stream.of(
                //Update name from null (null->kate)
                Arguments.of(null, "Kate"),
                //Update user's name to another valid name (kate->kat
                Arguments.of("Kate", "Kat"),
                //Update name to duplicate name(kat->kat)
                Arguments.of("Kat", "Kat"),
                //Update name - use all type symbols(kat->Kate 1234567890:%;№"!?*()+=,/\'<>.-_)
                Arguments.of("Kat", "Kate 1234567890:%;№!?*()+=,/'<>.-_")
        );
    }

    public static Stream<Arguments> nameDataForCornerCases() {
        return Stream.of(
                //only special symbols
                Arguments.of("-:%;№!?*()+=,/\"'<>.-_"),
                //only numbers
                Arguments.of("1234567890")
        );
    }

    public static Stream<Arguments> nameDataForNegativeCases() {
        return Stream.of(
                //only special symbols
                Arguments.of(""),
                //only numbers
                Arguments.of("   ")
        );
    }

    @ParameterizedTest
    @MethodSource("validNameData")
    @DisplayName("Пользователь может изменить имя с null и с другого значения")
    public void userCanUpdateNameTest(String initialName, String newName) {
        // Если initialName не null, сначала устанавливаем его
        if (initialName != null) {
            //передаем в json initialName
            UpdateCustomerProfileRequest updateUser1 = UpdateCustomerProfileRequest.builder()
                    .name(initialName)
                    .build();
            new UpdateCustomerProfile(RequestSpecs.userAuthSpec(user1Token), initialName)
                    .put(updateUser1);
        }

        // Теперь меняем имя на новое
        UpdateCustomerProfileRequest updateUser1 = UpdateCustomerProfileRequest.builder()
                .name(newName)
                .build();
        new UpdateCustomerProfile(RequestSpecs.userAuthSpec(user1Token), newName)
        .put(updateUser1);
    }

    @Test
    @DisplayName("Пользователь может изменить себе имя на имя у другого пользователя")
    public void userCanUpdateNameToNameAnotherUserTest() {
        String duplicateName = "UserKate";
        // Задаем имя первому пользователю
        UpdateCustomerProfileRequest user1update = UpdateCustomerProfileRequest.builder()
                .name(duplicateName)
                .build();
        given()
                .spec(RequestSpecs.userAuthSpec(user1Token))
                .body(user1update)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnOkAndCheckNewName(duplicateName));

        // Задаем имя первого пользователя второму
        UpdateCustomerProfileRequest user2update = UpdateCustomerProfileRequest.builder()
                .name(duplicateName)
                .build();
        given()
                .spec(RequestSpecs.userAuthSpec(user2Token))
                .body(user1update)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnOkAndCheckNewName(duplicateName));
    }

    @ParameterizedTest
    @MethodSource("nameDataForCornerCases")
    @DisplayName("Пользователь может изменить имя на значение только из символов или чисел")
    public void useOnlySpecialSymbolsOrNumbersForNameTest(String newName) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                //Вручную: JSON валиден, кавычки правильно экранированы
                //В тесте: String.format() может некорректно обработать \" и ' в строке
                .body(String.format("""
                        {
                        "name":"%s"
                        }
                        """, newName.replace("\"", "\\\"")))// ← Экранируем кавычки!
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", equalTo(newName));

    }

    @ParameterizedTest
    @MethodSource("nameDataForNegativeCases")
    @DisplayName("Пользователь не может изменить имя на невалидное значение(пустое, только пробелы)")
    public void userCannotUpdateNameWithInvalidValue(String newName) {
        UpdateCustomerProfileRequest user1update = UpdateCustomerProfileRequest.builder()
                .name(newName)
                .build();
        given()
                .spec(RequestSpecs.userAuthSpec(user1Token))
                .body(user1update)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsBadRequest());
    }

}