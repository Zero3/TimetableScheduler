package util;

import java.util.LinkedList;
import java.util.List;

public class PaddingStringBuilder
{
	private static final char DEFAULT_PADDING_CHAR = ' ';
	private final List<PadableString> strings = new LinkedList<>();
	private final List<PaddingSection> sections = new LinkedList<>();

	public PaddingStringBuilder append(PaddingSection section, Object obj)
	{
		if (obj != null)
		{
			String objString = obj.toString();
			strings.add(new PadableString(objString));

			section.length = Math.max(section.length, objString.length());
			sections.add(section);
		}

		return this;
	}

	public PaddingStringBuilder append(PaddingSection section, char padChar)
	{
		strings.add(new PadableString(padChar));
		sections.add(section);

		return this;
	}
	
	public PaddingStringBuilder append(Object obj)
	{
		if (obj != null)
		{
			strings.add(new PadableString(obj.toString()));
			sections.add(new PaddingSection(obj.toString().length()));
		}

		return this;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		while (!strings.isEmpty())
		{
			PadableString padableString = strings.remove(0);
			PaddingSection section = sections.remove(0);

			if (section.rightPad)
			{
				sb.append(padableString.string);
			}

			for (int i = 0; i < section.length - padableString.string.length(); i++)
			{
				sb.append(padableString.padChar);
			}

			if (!section.rightPad)
			{
				sb.append(padableString.string);
			}
		}

		return sb.toString();
	}
	
	private static class PadableString
	{
		public final String string;
		public final char padChar;
		
		public PadableString(String string)
		{
			this(string, DEFAULT_PADDING_CHAR);
		}
		
		public PadableString(char padChar)
		{
			this("", padChar);
		}
		
		public PadableString(String string, char padChar)
		{
			this.string = string;
			this.padChar = padChar;
		}
	}

	public static class PaddingSection
	{
		public int length;				// Will be updated by the PaddingStringBuilder on-the-run
		public final boolean rightPad;

		public PaddingSection(int length)
		{
			this(length, true);
		}

		public PaddingSection(boolean rightPad)
		{
			this(0, rightPad);
		}

		public PaddingSection(int length, boolean rightPad)
		{
			this.length = length;
			this.rightPad = rightPad;
		}
	}
}
