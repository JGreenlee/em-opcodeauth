import { NativeModules } from 'react-native';

const moduleName = 'OPCodeAuth';

const module = NativeModules[moduleName]
  ? NativeModules[moduleName]
  : new Proxy(
      {},
      {
        get() {
          throw new Error(`Native module ${moduleName} is not linked.`);
        },
      },
    );

export default {
  getOPCode: (): Promise<string> => module.getOPCode(),
  setOPCode: (opcode: string): Promise<void> => module.setOPCode(opcode),
};
