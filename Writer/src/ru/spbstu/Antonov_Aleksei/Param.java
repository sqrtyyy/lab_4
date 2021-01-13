package ru.spbstu.Antonov_Aleksei;

public interface Param {
    ErrorMessage isValid(String value);
    String toString();
    void setValue(Object obj, String value);
}
