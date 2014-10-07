package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import util.PaddingStringBuilder.PaddingSection;

// This is an utility class able to generate ASCII tables like this one:
// +-------------------------+----------+----------+----------+----------+
// | Table title / row title | Column 2 | Column 3 | Column 4 | Column 5 |
// +-------------------------+----------+----------+----------+----------+
// | Cell                    |     Cell |     Cell |     Cell |     Cell |
// | Cell                    |     Cell |     Cell |     Cell |     Cell |
// | Cell                    |     Cell |     Cell |     Cell |     Cell |
// +-------------------------+----------+----------+----------+----------+
// It takes advantage of PaddingStringBuilder to automatically pad cells within a column so that they have equal width.
public class AsciiTable
{
	List<PaddingSection> sections = new ArrayList<>();	// The padding sections used by PaddingStringBuilder to keep track of which cells should have equal width
	List<List<Object>> rows = new ArrayList<>();		// Contains the actual table contents
	
	// The number of rightPadding settings determines the number of columns in the table
	public AsciiTable(boolean... rightPaddings)
	{
		for (boolean rightPadding : rightPaddings)
		{
			sections.add(new PaddingSection(rightPadding));
		}
	}
	
	public void addRow(Object... cells)
	{
		if (cells.length > sections.size())
		{
			throw new IllegalArgumentException("Cannot add a row with more cells than the number of sections");
		}
		
		rows.add(Arrays.asList(cells));
	}
	
	public void addDelimiter()
	{
		rows.add(null);
	}

	@Override
	public String toString()
	{
		// Do the magic, using PaddingStringBuilder
		PaddingStringBuilder text = new PaddingStringBuilder();
		appendDelimiter(text, true);	// Bottom border
		
		for (List<Object> row : rows)
		{
			if (row == null)
			{
				appendDelimiter(text, true);
			}
			else
			{
				text.append("| ");
				
				for (int section = 0; section < sections.size(); section++)
				{
					if (section > 0)
					{
						text.append(" | ");
					}
					
					text.append(sections.get(section), row.get(section));
				}
				
				text.append(" |").append(System.lineSeparator());
			}
		}
		
		appendDelimiter(text, false);	// Bottom border
		
		return text.toString();
	}
	
	private void appendDelimiter(PaddingStringBuilder text, boolean addNewline)
	{
		text.append("+-");
		
		for (int section = 0; section < sections.size(); section++)
		{
			if (section > 0)
			{
				text.append("-+-");
			}
			
			text.append(sections.get(section), '-');
		}
		
		text.append("-+");
		
		if (addNewline)
		{
			text.append(System.lineSeparator());
		}
	}
}