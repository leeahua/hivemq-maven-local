package az;

public abstract class AbstractInformation {
    protected StringBuilder addInformation(StringBuilder builder, String key, String value) {
        return builder.append(String.format("[%s] = [%s]\n", key, value));
    }
}
