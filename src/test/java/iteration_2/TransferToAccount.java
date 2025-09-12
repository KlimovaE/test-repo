package iteration_2;

import io.restassured.path.json.JsonPath;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import spec.RequestSpecs;
import spec.ResponseSpecs;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferToAccount {
    String adminToken;
    String user1Token;
    String user2Token;
    //Генерация уникальных userName для каждого теста
    String user1Username = "A_" + System.currentTimeMillis(); // "kate001_123456"
    String user2Username = "B_" + System.currentTimeMillis(); // "kate002_123456"
    String user1Password = "Kate012!";
    String user2Password = "Kate013!";
    static long firstAccountUser1;
    static long secondAccountUser1;
    static long firstAccountUser2;
    String userRole = "USER";
    Random random = new Random();
    static long randomNonExistentId = new Random().nextInt(100000, 1000000);

    //Вынесен отдельно метод по созданию аккаунта
    public long createAccount(String userToken) {
        CreateAccountRequest createAccountRequest = CreateAccountRequest.builder()
                .build();
        return new CreateAccount(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.entityWasCreated())
                .post(createAccountRequest)
                .extract()
                .path("id");
    }

    @BeforeEach
    public void setupTestData() {
        //Создание первого пользователя
        CreateUserRequest createUser1 = CreateUserRequest.builder()
                .username(user1Username)
                .password(user1Password)
                .role(userRole)
                .build();
        new CreateUser(RequestSpecs.adminAuthSpec(), ResponseSpecs.entityWasCreated())
                .post(createUser1);
        //Создание второго пользователя
        CreateUserRequest createUser2 = CreateUserRequest.builder()
                .username(user2Username)
                .password(user2Password)
                .role(userRole)
                .build();
        new CreateUser(RequestSpecs.adminAuthSpec(), ResponseSpecs.entityWasCreated())
                .post(createUser2);

        //Получение токена для пользователя1
        LoginUserRequest loginUser1 = LoginUserRequest.builder()
                .username(user1Username)
                .password(user1Password)
                .build();
        user1Token = new AdminLoginUserRequest(RequestSpecs.unAuthSpec(), ResponseSpecs.requestReturnsOK())
                .post(loginUser1)
                .extract()
                .header("Authorization");

        //Получение токена для пользователя2
        LoginUserRequest loginUser2 = LoginUserRequest.builder()
                .username(user2Username)
                .password(user2Password)
                .build();
        user2Token = new AdminLoginUserRequest(RequestSpecs.unAuthSpec(), ResponseSpecs.requestReturnsOK())
                .post(loginUser2)
                .extract()
                .header("Authorization");

        //Создание 2х счетов для пользователя 1 и 1 счета для пользователя 2
        firstAccountUser1 = createAccount(user1Token);
        secondAccountUser1 = createAccount(user1Token);
        firstAccountUser2 = createAccount(user2Token);
    }

    //Метод по предварительному пополнению баланса
    private void depositUserAccount(String userToken, long userAccount, double balance) {
        DepositAccountRequest depositAccountRequest = DepositAccountRequest.builder()
                .id(userAccount)
                .balance(balance)
                .build();
        new UserDepositAccount(RequestSpecs.userAuthSpec(userToken), ResponseSpecs.requestReturnsOK())
                .post(depositAccountRequest);

    }

    public static Stream<Arguments> transactionDataForPositiveCase() {
        return Stream.of(
                Arguments.of(firstAccountUser1, secondAccountUser1, 300, 0, 100, 200, 100),
                Arguments.of(firstAccountUser1, secondAccountUser1, 500, 100, 150, 350, 250),
                Arguments.of(firstAccountUser1, secondAccountUser1, 300, 0, 0.01, 299.99, 0.01),
                Arguments.of(firstAccountUser1, secondAccountUser1, 300, 0, 300, 0, 300),
                Arguments.of(firstAccountUser1, firstAccountUser2, 300, 0, 0.01, 299.99, 0.01)//перевод на счет другого пользователя
        );
    }

    public static Stream<Arguments> transactionDataAmountForNegativeCase() {
        return Stream.of(
                Arguments.of(firstAccountUser1, secondAccountUser1, 300, 300.01),
                Arguments.of(firstAccountUser1, secondAccountUser1, 500, -100),
                Arguments.of(firstAccountUser1, secondAccountUser1, 300, 0)
        );
    }

    public static Stream<Arguments> transactionDataAccountForNegativeCase() {
        return Stream.of(
                //С несуществующего счета
                Arguments.of(randomNonExistentId, secondAccountUser1, 300.0, HttpStatus.SC_FORBIDDEN),
                //На несуществующий счет
                Arguments.of(firstAccountUser1, randomNonExistentId, 300.0, HttpStatus.SC_FORBIDDEN),
                //Со счета другого пользователя
                Arguments.of(firstAccountUser1, secondAccountUser1, 300.0, HttpStatus.SC_FORBIDDEN)

        );
    }


    private String getUserAmountsInfo(String userToken) {
        return new GetCustomerProfileRequest(RequestSpecs.userAuthSpec(userToken), ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .asString();// ← получаем как строку
    }

    @ParameterizedTest
    @DisplayName("Успешное пополнение счета первый и последующие разы")
    @MethodSource("transactionDataForPositiveCase")
    public void userCanTransitTest(long senderAccount, long receiverAccount, double initialSenderAccountBalance,
                                   double initialReceiverAccountBalance,
                                   double transferAmount, double expectedSenderAccountBalance, double expectedReceiverAccountBalance) {

        // 1. Устанавливаем начальный баланс (если нужно)
        depositUserAccount(user1Token, senderAccount, initialSenderAccountBalance);
        if (initialReceiverAccountBalance != 0) {
            depositUserAccount(user1Token, receiverAccount, initialReceiverAccountBalance);
        }

        // 2. Делаем перевод
        TransferAccountRequest transferAccountRequest = TransferAccountRequest.builder()
                .senderAccountId(senderAccount)
                .receiverAccountId(receiverAccount)
                .amount(transferAmount)
                .build();

        new TransferToAccountRequest(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsOK())
                .post(transferAccountRequest);

        Double actualBalanceSenderAccount = JsonPath.from(getUserAmountsInfo(user1Token))
                .getDouble("find { it.id == " + senderAccount + " }.balance");
        Double actualBalanceReceiverAccount = JsonPath.from(getUserAmountsInfo(user1Token))
                .getDouble("find { it.id == " + receiverAccount + " }.balance");

        assertEquals(actualBalanceSenderAccount, expectedSenderAccountBalance, 0.01);
        assertEquals(actualBalanceReceiverAccount, expectedReceiverAccountBalance, 0.01);

    }

    @ParameterizedTest
    @DisplayName("Неуспешный перевод с невалидной суммой")
    @MethodSource("transactionDataAmountForNegativeCase")
    public void userCannotTransitInvalidAmountTest(long senderAccount, long receiverAccount, double initialSenderAccountBalance,
                                                   double transferAmount) {
        // 1. Устанавливаем начальный баланс (если нужно)
        depositUserAccount(user1Token, senderAccount, initialSenderAccountBalance);

        // 2. Делаем перевод
        TransferAccountRequest transferAccountRequest = TransferAccountRequest.builder()
                .senderAccountId(senderAccount)
                .receiverAccountId(receiverAccount)
                .amount(transferAmount)
                .build();

        new TransferToAccountRequest(RequestSpecs.userAuthSpec(user1Token), ResponseSpecs.requestReturnsBadRequest())
                .post(transferAccountRequest);

    }

    @ParameterizedTest
    @DisplayName("Неуспешный перевод с невалидными счетами")
    @MethodSource("transactionDataAccountForNegativeCase")
    public void userCannotTransferTest(long senderAccount, long receiverAccount, double transferAmount, int expectedStatusCode) {
        TransferAccountRequest transferAccountRequest = TransferAccountRequest.builder()
                .senderAccountId(senderAccount)
                .receiverAccountId(receiverAccount)
                .amount(transferAmount)
                .build();

        int actualStatusCode = new TransferToAccountRequest(RequestSpecs.userAuthSpec(user1Token),
                ResponseSpecs.requestReturnsForbidden())
                .post(transferAccountRequest)
                .extract()
                .statusCode();
        assertEquals(expectedStatusCode,actualStatusCode, "Перевод с невалидными счетами возвратит 401 ошибку");
    }

    @Test
    @DisplayName("Ошибка перевод денег на тот же счет, с которого и переводят")
    public void userCannotTransferWhenSenderAccountAndReceiverAccountAreEqual() {
        depositUserAccount(user1Token, firstAccountUser1, 500);
        TransferAccountRequest transferAccountRequest = TransferAccountRequest.builder()
                .senderAccountId(firstAccountUser1)
                .receiverAccountId(firstAccountUser1)
                .amount(100)
                .build();

        new TransferToAccountRequest(RequestSpecs.userAuthSpec(user1Token),
                ResponseSpecs.requestReturnsBadRequest())
                .post(transferAccountRequest);
    }
}