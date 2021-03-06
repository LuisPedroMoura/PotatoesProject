/***************************************************************************************
*	Title: PotatoesProject - Unit Class Source Code
*	Code version: 2.0
*	Author: Luis Moura (https://github.com/LuisPedroMoura)
*	Co-author in version 1.0: Pedro Teixeira (https://pedrovt.github.io)
*	Acknowledgments for version 1.0: Maria Joao Lavoura (https://github.com/mariajoaolavoura),
*	for the help in brainstorming the concepts needed to create the first working version
*	of this Class that could deal with different unit Variables operations.
*	Date: July-2018
*	Availability: https://github.com/LuisPedroMoura/PotatoesProject
*
***************************************************************************************/

package unitsGrammar.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * <b>Unit</b><p>
 * 
 * @author Luis Moura (https://github.com/LuisPedroMoura)
 * @version 2.0 - July 2018
 */
public class Unit {

	// Static Fields
	private static int newCode = 1;

	// --------------------------------------------------------------------------
	// Instance Fields
	private String name;
	private String symbol;
	private Code code;
	private boolean isClass;
	private boolean isStructure;

	// --------------------------------------------------------------------------
	// CTORs
	
	/**
	 * Constructor for basic units<p>
	 * Create a new basic unit - code is generated automatically
	 * @param name		for example 'meter'
	 * @param symbol	for example 'm'
	 */
	protected Unit(String name, String symbol) {
		this(name, symbol, new Code(++newCode)); // ++ operator before variable ensures that code 1 is never used and can be saved for dimensionless unit 'number'
	}

	/**
	 * Constructor for derived units<p>
	 * Create a new unit based on another unit - usually @code will be obtained by a units operation
	 * @param name	for example 'meter'
	 * @param symbol	for example 'm'
	 * @param code usually obtained in a Unit operation
	 */
	protected Unit(String name, String symbol, Code code) {
		this(name, symbol, code, false, false);
	}
	
	/**
	 * Constructor for special Units<p>
	 * Classes o Units and Structures are treated internally as Units.
	 * They have to be tagged to be distinguished in graph traversals and path calculations
	 * @param name
	 * @param symbol
	 * @param code
	 * @param isClass
	 * @param isStructure
	 */
	protected Unit(String name, String symbol, Code code, boolean isClass, boolean isStructure) {
		this.name = name;
		this.symbol = symbol;
		this.code = code;
		this.isClass = isClass;
		this.isStructure = isStructure;
	}

	/**
	 * Constructor for temporary units<p>
	 * Creates a new unit with name = "temp" and symbol = "". The code will be deep copied.
	 * Create a new unit based on another unit 
	 * @param code a unique prime number, or the result of operating with other codes
	 */
	protected Unit(Code calculatedCode) {
		this.name  = "";
		this.symbol = "";
		this.code = new Code(calculatedCode);
	}

	/** 
	 * Copy Constructor 
	 * @param unit 
	 * @throws NullPointerException if unit is null
	 */ 
	public Unit(Unit unit) { 
		this.name  = unit.name; 
		this.symbol = unit.symbol;
		this.code = new Code(unit.getCode());
	}

	// --------------------------------------------------------------------------
	// Getters 
	
	/**
	 * @return name, the name of this Unit.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return symbol, the symbol of this Unit.
	 */
	public String getSymbol() {
		return symbol;
	}
	
	/**
	 * @return the Code of this Unit.
	 */
	protected Code getCode() {
		return code;
	}
	
	/**
	 * @return true if this Unit represents a Class of Units ("dimension").
	 */
	public boolean isClass() {
		return isClass;
	}
	
	/**
	 * sets this Unit as a Class of Units ("dimension") (this is not reversible).
	 */
	protected void setAsClass() {
		this.isClass = true;
	}
	
	/**
	 * @return true is this Unit represents a structure ("multi-dimensional unit").
	 */
	public boolean isStructure() {
		return isStructure;
	}
	
	/**
	 * sets this Unit as Structure (this is not reversible).
	 */
	protected void setAsStructure() {
		this.isStructure = true;
	}

	// --------------------------------------------------------------------------
	// Other Methods
	
	/**
	 * After operating with units, the resulting Unit may not be a defined one.
	 * example: if volume is defined as m^3, the unit yd*m^2 also represents volume, as meter and yards measure the same dimension.
	 * Some conversions will be needed if Quantities are associated with the units.
	 * This method tries to find an equivalent Unit defined by in the Units File, and convert 'this'.
	 * @return the conversion factor generated by converting this Unit. To be applied if Quantities are associated with the Unit.
	 * @throws IllegalArgumentException if no conversion is possible.
	 */
	public double adjustToKnownUnit() {
		
		Map<Unit, Map<Unit, Double>> conversionTable = Units.getConversionTable();
		Map<String, Unit> unitsTable = Units.getUnitsTable();
		Map<Integer, Unit> codesTable = Units.getBasicUnitsCodesTable();

		// first tries to simplify this Unit code using conversions ('m^2/yd' -> 'm')
		// if simplification occurs, a conversion factor is given for quantity adjustment
		double conversionFactor = this.code.simplifyCodeWithConvertions(conversionTable, codesTable);
		double auxConvFactor = 0.0;
		// search for Unit with same Code in unitsTable
		// if search fails, conversions might still be needed
		
		for (String key : unitsTable.keySet()) {
			Unit unit = unitsTable.get(key);
			if (this.code.equals(unit.getCode())) {
				this.name = unit.getName();
				this.symbol = unit.getSymbol();
				return conversionFactor;
			}
		}
		
		
		for (String key : unitsTable.keySet()) {
			try {
				auxConvFactor *= Code.matchCodes(unitsTable.get(key).getCode(), this.getCode(), conversionTable, codesTable); // throws IllegalArgumentException
				Unit unit = unitsTable.get(key);
				this.code = unit.getCode();
				this.name = unit.getName();
				this.symbol = unit.getSymbol();
			}
			catch (IllegalArgumentException e) {
				continue;
			}
		}

		// a conversion was not possible, there is no defined Unit that matches this.
		if (auxConvFactor == 0.0) {
			this.name = "";
			this.symbol = generateMathematicalSymbol();
			return conversionFactor;
		}
		
		// conversion was possible
		conversionFactor *= auxConvFactor;
		return conversionFactor;
	}
	
	/**
	 * When adding or subtracting units some conversions might be needed.
	 * The same applies to assignments or casts of a Unit.
	 * Example: 'm + yd'; '(m) yd'; 'm = yd + ft' . Meter and Yard are equivalent Units that measure the same dimension.
	 * To add both correctly, one needs to be converted to the other. Same principle applies in the other examples.
	 * @param a, the destination Unit that 'this' is going to be converted to
	 * @return the conversion factor generated by converting this Unit. To be applied if Quantities are asociated with the Unit.
	 * @throws IllegalArgumentException the match is not possible
	 */
	public double matchUnitTo(Unit a) throws IllegalArgumentException {
		
		Map<Unit, Map<Unit, Double>> conversionTable = Units.getConversionTable();
		Map<Integer, Unit> codesTable = Units.getBasicUnitsCodesTable();
		
		Double factor = null;
		
		// if there is a conversion factor between the Units, then the conversion is direct
		if (conversionTable.get(this) != null) {
			factor = conversionTable.get(this).get(a);
			
			// if factor is infinity, there is no possible conversion to be made
			if (factor == Double.POSITIVE_INFINITY) {
				throw new IllegalArgumentException();
			}
		}
		
		// if no conversion factor exists, Units might need to be converted to get one
		if (factor == null) {
			factor = Code.matchCodes(a.getCode(), this.getCode(), conversionTable, codesTable); // throws IllegalArgumentException
		}
		
		this.code = a.getCode();
		this.name = a.getName();
		this.symbol = a.getSymbol();
		
		return factor;
	}
	
	public boolean isCompatible(Unit a) {
		
		Map<Unit, Map<Unit, Double>> conversionTable = Units.getConversionTable();
		Map<Integer, Unit> codesTable = Units.getBasicUnitsCodesTable();
		Double factor = null;
		
		// if there is a conversion factor between the Units, then they are compatible
		if (conversionTable.get(this) != null) {
			factor = conversionTable.get(this).get(a);
			
			// if factor is infinity, Units are not compatible
			if (factor == Double.POSITIVE_INFINITY) {
				return false;
			}
		}
		
		// conversion factor is not null and not infinite, Units are compatible
		if (factor != null) {
			return true;
		}
		
		// if no conversion factor exists, Units might need to be converted to get one
		try {
			// conversion is possible
			Code.matchCodes(a.getCode(), this.getCode(), conversionTable, codesTable); // throws IllegalArgumentException
		}
		catch (IllegalArgumentException e) {
			// conversion is not possible
			return false;
		}
		
		return true;
	}
	
	/**
	 * After operating with units, the result might be an unknown or not defined Unit. it might still be useful for other operations,
	 * and it may be necessary to print its value. In that case, although the Unit will not have a name, because it wasn't
	 * defined, a mathematical symbol can be generated from its code.
	 * @return a String with the mathematical symbol that represents this Unit.
	 */
	private String generateMathematicalSymbol() {
		
		String symbol = "";
		
		List<Integer> used = new ArrayList<>();
		List<Integer> numCodes = this.getCode().getNumCodes();
		List<Integer> denCodes = this.getCode().getDenCodes();
		
		for (Integer code : numCodes) {
			if(!used.contains(code)) {
				int count = (int) numCodes.stream().filter(c -> c == code).count();
				used.add(code);
				symbol += Units.getBasicUnitsCodesTable().get(code).getSymbol();
				if (count > 1) {
					symbol += "^" + count;
				}
				symbol += " ";
			}
		}
		
		used.clear();
		for (Integer code : denCodes) {
			if(!used.contains(code)) {
				int count = (int) denCodes.stream().filter(c -> c == code).count();
				used.add(code);
				symbol += Units.getBasicUnitsCodesTable().get(code).getSymbol();
				symbol += "^-" + count;
				symbol += " ";
			}
		}
		
		return symbol.substring(0, symbol.length()-1);
	}
	

	@Override
	public String toString() {

		// For Debug Purposes Only
		StringBuilder builder = new StringBuilder();
		builder.append("Unit [");
		if (name != null) {
			builder.append(name);
			builder.append(", ");
		}
		if (symbol != null) {
			builder.append(symbol);
			builder.append(", ");
		}
		builder.append(code);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Unit other = (Unit) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}