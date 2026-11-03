import { useDispatch, useSelector } from "react-redux";
import type { AppDispatch, RootState } from "./store";

/**
 * `useDispatch` and `useSelector` with the app's store types bound once, so
 * components neither repeat `<AppDispatch>` nor `(state: RootState)`.
 */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector = useSelector.withTypes<RootState>();
