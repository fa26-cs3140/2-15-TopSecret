package org.example;

public enum AccessLevel 
{
    Admin(0),
    Standard(1);

    private final int value;

    AccessLevel(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return this.value;
    }
}
