package net.minecraft.util.text;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

public abstract class TextComponentBase implements ITextComponent {
   protected List<ITextComponent> siblings = Lists.newArrayList();
   private Style style;

   @Override
   public ITextComponent appendSibling(ITextComponent component) {
      component.getStyle().setParentStyle(this.getStyle());
      this.siblings.add(component);
      return this;
   }

   @Override
   public List<ITextComponent> getSiblings() {
      return this.siblings;
   }

   @Override
   public ITextComponent appendText(String text) {
      return this.appendSibling(new TextComponentString(text));
   }

   @Override
   public ITextComponent setStyle(Style style) {
      this.style = style;

      for (ITextComponent itextcomponent : this.siblings) {
         itextcomponent.getStyle().setParentStyle(this.getStyle());
      }

      return this;
   }

   @Override
   public Style getStyle() {
      if (this.style == null) {
         this.style = new Style();

         for (ITextComponent itextcomponent : this.siblings) {
            itextcomponent.getStyle().setParentStyle(this.style);
         }
      }

      return this.style;
   }

   @Override
   public Iterator<ITextComponent> iterator() {
      return Iterators.concat(Iterators.forArray(this), createDeepCopyIterator(this.siblings));
   }

   @Override
   public final String getUnformattedText() {
      StringBuilder stringbuilder = new StringBuilder();

      for (ITextComponent itextcomponent : this) {
         stringbuilder.append(itextcomponent.getUnformattedComponentText());
      }

      return stringbuilder.toString();
   }

   @Override
   public final String getFormattedText() {
      StringBuilder stringbuilder = new StringBuilder();

      for (ITextComponent itextcomponent : this) {
         String s = itextcomponent.getUnformattedComponentText();
         if (!s.isEmpty()) {
            stringbuilder.append(itextcomponent.getStyle().getFormattingCode());
            stringbuilder.append(s);
            stringbuilder.append(TextFormatting.RESET);
         }
      }

      return stringbuilder.toString();
   }

   public static Iterator<ITextComponent> createDeepCopyIterator(Iterable<ITextComponent> components) {
      Iterator<ITextComponent> iterator = Iterators.concat(
         Iterators.transform(components.iterator(), new Function<ITextComponent, Iterator<ITextComponent>>() {
            public Iterator<ITextComponent> apply(@Nullable ITextComponent p_apply_1_) {
               return p_apply_1_.iterator();
            }
         })
      );
      return Iterators.transform(iterator, new Function<ITextComponent, ITextComponent>() {
         public ITextComponent apply(@Nullable ITextComponent p_apply_1_) {
            ITextComponent itextcomponent = p_apply_1_.createCopy();
            itextcomponent.setStyle(itextcomponent.getStyle().createDeepCopy());
            return itextcomponent;
         }
      });
   }

   @Override
   public boolean equals(Object p_equals_1_) {
      if (this == p_equals_1_) {
         return true;
      } else {
         return !(p_equals_1_ instanceof TextComponentBase textcomponentbase)
            ? false
            : this.siblings.equals(textcomponentbase.siblings) && this.getStyle().equals(textcomponentbase.getStyle());
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.style.hashCode() + this.siblings.hashCode();
   }

   @Override
   public String toString() {
      return "BaseComponent{style=" + this.style + ", siblings=" + this.siblings + "}";
   }
}
