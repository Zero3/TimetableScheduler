package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;

public class Tools
{
	public static String nameConcat(Object... objects)
	{
		StringBuilder result = new StringBuilder();

		for (Object obj : objects)
		{
			if (result.length() != 0)
			{
				result.append("_");
			}

			result.append(obj.toString());
		}

		return result.toString();
	}
	
	public static boolean deleteRecursive(File path, boolean deleteSelf) throws FileNotFoundException
	{
		if (!path.exists())
		{
			throw new FileNotFoundException(path.getAbsolutePath());
		}
		
		boolean ret = true;
		
		if (path.isDirectory())
		{
			for (File f : path.listFiles())
			{
				ret = ret && deleteRecursive(f, true);
			}
		}
		
		if (deleteSelf)
		{
			return ret && path.delete();
		}
		else
		{
			return ret;
		}
	}
	
	private static final DecimalFormat DOUBLE_ZERO_PAD_FORMAT = new DecimalFormat("00");
	
	public static String doubleZeroPad(int number)
	{
		return DOUBLE_ZERO_PAD_FORMAT.format(number);
	}
	
	public static String timeString(long seconds)
	{
		return String.format("%d:%02d:%02d", seconds/3600, (seconds % 3600) / 60, (seconds % 60));
	}
}