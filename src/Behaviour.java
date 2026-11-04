public interface Behaviour<T extends MovingEntity> {
    public void calculate(T self, World world);
}
