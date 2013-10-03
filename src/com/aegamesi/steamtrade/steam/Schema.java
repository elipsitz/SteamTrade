package com.aegamesi.steamtrade.steam;

import java.util.Map;

public class Schema {
	public Map<Integer, SchemaItem> items;
	public Map<Integer, SchemaAttribute> attributes;
	public Map<Integer, SchemaParticle> particles;

	public class SchemaItem {
		public String item_name;
		public String item_type_name;
		public int defindex;
		public boolean proper_name;
		public String image_url;
		public String image_url_large;
		public String item_description;
	}

	public class SchemaAttribute {
		public boolean stored_as_integer;
		public String effect_type = "neutral";
		public String description_string;
		public String description_format;
		public int defindex;
	}

	public class SchemaParticle {
		public int id;
		public String name;
	}

	public enum SchemaPaint {
		PAINT1(3100495, "A Color Similar to Slate"), //
		PAINT2(7511618, "Indubitably Green"), //
		PAINT3(8208497, "A Deep Commitment to Purple"), //
		PAINT4(13595446, "Mann Co. Orange"), //
		PAINT5(1315860, "A Distinctive Lack of Hue"), //
		PAINT6(10843461, "Muskelmannbraun"), //
		PAINT7(12377523, "A Mann's Mint"), //
		PAINT8(5322826, "Noble Hatter's Violet"), //
		PAINT9(2960676, "After Eight"), //
		PAINT10(12955537, "Peculiarly Drab Tincture"), //
		PAINT11(8289918, "Aged Moustache Grey"), //
		PAINT12(16738740, "Pink as Hell"), //
		PAINT13(15132390, "An Extraordinary Abundance of Tinge"), //
		PAINT14(6901050, "Radigan Conagher Brown"), //
		PAINT15(15185211, "Australium Gold"), //
		PAINT16(3329330, "The Bitter Taste of Defeat and Lime"), //
		PAINT17(14204632, "Color No. 216-190-216"), //
		PAINT18(15787660, "The Color of a Gentlemann's Business Pants"), //
		PAINT19(15308410, "Dark Salmon Injustice"), //
		PAINT20(8154199, "Ye Olde Rustic Colour"), //
		PAINT21(8421376, "Drably Olive"), //
		PAINT22(4345659, "Zepheniah's Greed"), //
		PAINT23(6637376, 2636109, "An Air of Debonair"), //
		PAINT24(12073019, 5801378, "Team Spirit"), //
		PAINT25(3874595, 1581885, "Balaclavas Are Forever"), //
		PAINT26(8400928, 2452877, "The Value of Teamwork"), //
		PAINT27(12807213, 12091445, "Cream Spirit"), //
		PAINT28(11049612, 8626083, "Waterlogged Lab Coat"), //
		PAINT29(4732984, 3686984, "Operator's Overalls"), //
		UNKNOWN(-1, -1, "Unknown"); //

		public String name;
		public int color1 = -1;
		public int color2 = -1;

		private SchemaPaint(int color1, int color2, String name) {
			this.name = name;
			this.color1 = color1;
			this.color2 = color2;
		}

		private SchemaPaint(int color, String name) {
			this.name = name;
			this.color1 = color;
		}

		public static SchemaPaint getByColor(int color) {
			SchemaPaint paints[] = values();
			for (SchemaPaint paint : paints)
				if (paint.color1 == color || paint.color2 == color)
					return paint;
			return UNKNOWN;
		}
	}

	public enum SchemaQuality {
		NORMAL("Normal", false), //
		GENUINE("Genuine", true, 0xFF4F7455, 0xFF2F4332, 0xFF4C7554), //
		RARITY2, // unknown
		VINTAGE("Vintage", true, 0xFF476291, 0xFF354664, 0xFF456292), //
		RARITY3, // unknown
		UNUSUAL("Unusual", true, 0xFF8650AC, 0xFF664580, 0xFF8650AC), //
		UNIQUE("Unique", false, 0xFFFFD700, 0xFFAC920E, 0xFFFFD800), //
		COMMUNITY("Community", true, 0xFF70B04A, 0xFF559131, 0xFF70B04A), //
		VALVE("Valve", true, 0xFFA50F79, 0xFF880A63, 0xFFA50F79), //
		SELFMADE("Self-Made", true, 0xFF70B04A, 0xFF559131, 0xFF70B04A), //
		CUSTOMIZED, // unknown
		STRANGE("Strange", true, 0xFFCF6A32, 0xFF8D4B26, 0xFFD06A2E), //
		COMPLETED, // unknown
		HAUNTED("Haunted", true, 0xFF38F3AB, 0xFF309168, 0xFF38F3AB);//

		public int color = 0xFFB2B2B2;
		public int bgColor = 0xFFB2B2B2;
		public int outlineColor = 0xFFFFFFFF;
		public String name = "";
		public boolean prefix = false;
		public static SchemaQuality[] values = SchemaQuality.values();

		private SchemaQuality() {
		}

		private SchemaQuality(String name, boolean prefix) {
			this.name = name;
			this.prefix = prefix;
		}

		private SchemaQuality(String name, boolean prefix, int color, int bgColor, int outlineColor) {
			this.name = name;
			this.prefix = prefix;
			this.color = color;
			this.bgColor = bgColor;
			this.outlineColor = outlineColor;
		}
	}
}
