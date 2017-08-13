package net.tonbot.plugin.ifplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

import lombok.Getter;

/**
 * Invariant:
 * cursorY must always point to an existent index.
 * cursorX doesn't have to point to an existing index. (But if cursorX is > 0, then cursorX-1 must be pointing to an existing index.)
 */
class CharacterMatrix {

	@Getter
	private final Integer maxWidth;
	
	@Getter
	private Integer maxHeight;
	
	protected List<List<Character>> matrix = null;
	
	protected int cursorX;
	protected int cursorY;
	
	/**
	 * Creates a new matrix with an unconstrained width and height.
	 */
	public CharacterMatrix() {
		this.maxWidth = null;
		this.maxHeight = null;
		
		reset();
	}
	
	/**
	 * Creates a new matrix with a given maximum width and height.
	 * @param maxWidth Max width must be > 0.
	 * @param maxHeight Max height must be >= 0.
	 */
	public CharacterMatrix(int maxWidth, int maxHeight) {
		Preconditions.checkArgument(maxWidth > 0, "maxWidth must be positive.");
		Preconditions.checkArgument(maxHeight >= 0, "maxHeight must be non-negative.");
		this.maxWidth = maxWidth; 
		this.maxHeight = maxHeight;
		
		reset();
	}
	
	/**
	 * Sets the maximum height. If the height has been shortened, then all lines beyond the new maxHeight will be cleared. 
	 * The cursor will not move. Hence, the cursor may be out of bounds after this operation.
	 * @param maxHeight The new max height. Must be greater than or equal to 0.
	 */
	public void setMaxHeight(int maxHeight) {
		Preconditions.checkArgument(maxHeight >= 0, "maxHeight must be non-negative.");
		this.maxHeight = maxHeight;
		
		// Removes all lines beyond maxHeight.
		while (matrix.size() > maxHeight) {
			matrix.remove(maxHeight-1);
		}
		
		// What should happen to the cursor? hmm...
	}
	
	/**
	 * Resets the position of the cursor and clears out all data. 
	 * Max height and width are preserved.
	 */
	public void reset() {
		this.cursorX = 0;
		this.cursorY = 0;
		
		this.matrix = new ArrayList<>();
		
		List<Character> row0 = new ArrayList<>();
		this.matrix.add(row0);
	}
	
	/**
	 * Gets the cursor position.
	 * @return An integer array of size 2. The number at index 0 is the line index (zero-indexed), 
	 *     while the number at index 1 is the column index (zero-indexed).
	 */
	public int[] getCursorPosition() {
		return new int[] {cursorY, cursorX};
	}
	
	/**
	 * Sets the cursor at a position in the matrix.
	 * @param y The 0-indexed row. Must be positive and < maxHeight.
	 * @param x The 0-indexed column. Must be >= 0 and < maxWidth.
	 */
	public void setCursor(int y, int x) {
		Preconditions.checkArgument(y >= 0, "y must be non-negative.");
		Preconditions.checkArgument(x >= 0, "x must be non-negative.");
		
		if (maxWidth != null) {
			Preconditions.checkArgument(x < maxWidth, "x must be less than the max width.");
		}
		
		if (maxHeight != null) {
			Preconditions.checkArgument(y < maxHeight, "y must be less than the max height.");
		}
		
		fillUpTo(matrix, y, () -> new ArrayList<>());
		this.cursorY = y;
		
		List<Character> line = matrix.get(this.cursorY);
		fillUpTo(line, Math.max(x-1, 0), () -> ' ');
		this.cursorX = x;
	}
	
	/**
	 * Writes a character to the matrix at the current cursor location. 
	 * If the character is a newline, then the cursor will be moved to the beginning of the next line disregarding 
	 * whether if there is anything already there in the next line.
	 * This operation is a no-op if the cursor out of bounds.
	 * 
	 * @param value The character to write.
	 */
	public void write(char value) {
		if ((maxWidth != null && this.cursorX >= maxWidth) || (maxHeight != null && this.cursorY >= maxHeight)) {
			return;
		}
		
		if (value == '\n') {
			int nextCursorY = this.cursorY + 1;
			if (maxHeight == null || nextCursorY < maxHeight) {
				setCursor(nextCursorY, 0);
			}
		} else {
			List<Character> line = matrix.get(this.cursorY);
			
			// If necessary, pad the line up with spaces until where the cursor is.
			fillUpTo(line, this.cursorX, () -> ' ');
			
			if (line.size() == this.cursorX) {
				line.add(value);
			} else {
				line.set(this.cursorX, value);
			}
			
			this.cursorX++;
		}
	}
	
	public String render() {
		List<String> listOfLines = matrix.stream()
		    .map(line -> {
				StringBuffer lineSb = new StringBuffer();
				line.forEach(character -> lineSb.append(character));
				return lineSb.toString();
		    })
		    .collect(Collectors.toList());
		
		String result = StringUtils.join(listOfLines, "\n");
		
		return result;
	}
	
	private <T> void fillUpTo(List<T> list, int upToIndex, Supplier<T> initialValueSupplier) {
		if (upToIndex >= list.size()) {
			int needMoreCount = upToIndex - list.size() + 1;
			for (int i = 0; i < needMoreCount; i++) {
				list.add(initialValueSupplier.get());
			}
		}
	}
}
