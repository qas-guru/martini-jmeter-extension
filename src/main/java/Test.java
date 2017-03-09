import java.util.ResourceBundle;

public class Test {
	public static void main(String[] args) {
		ResourceBundle resourceBundle = ResourceBundle.getBundle("qas.guru.martini.jmeter");
		String string = resourceBundle.getString("martini_pre_processor_label");
		System.out.println(string);

	}
}
