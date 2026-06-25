package net.optifine.entity.model.anim;

public class RenderResolverEntity implements IRenderResolver {
   @Override
   public IExpression getParameter(String name) {
      return EnumRenderParameterEntity.parse(name);
   }
}
